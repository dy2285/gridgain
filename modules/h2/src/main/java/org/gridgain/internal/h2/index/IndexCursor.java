/*
 * Copyright 2004-2019 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.gridgain.internal.h2.index;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import org.gridgain.internal.h2.engine.Session;
import org.gridgain.internal.h2.expression.condition.Comparison;
import org.gridgain.internal.h2.message.DbException;
import org.gridgain.internal.h2.result.ResultInterface;
import org.gridgain.internal.h2.result.Row;
import org.gridgain.internal.h2.result.SearchRow;
import org.gridgain.internal.h2.result.SortOrder;
import org.gridgain.internal.h2.table.Column;
import org.gridgain.internal.h2.table.IndexColumn;
import org.gridgain.internal.h2.table.Table;
import org.gridgain.internal.h2.table.TableFilter;
import org.gridgain.internal.h2.value.Value;
import org.gridgain.internal.h2.value.ValueGeometry;
import org.gridgain.internal.h2.value.ValueNull;

/**
 * The filter used to walk through an index. This class supports IN(..)
 * and IN(SELECT ...) optimizations.
 *
 * @author Thomas Mueller
 * @author Noel Grandin
 * @author Nicolas Fortin, Atelier SIG, IRSTV FR CNRS 24888
 */
public class IndexCursor implements Cursor, AutoCloseable {

    private final TableFilter tableFilter;
    private Index index;
    private Table table;
    private IndexColumn[] indexColumns;
    private boolean alwaysFalse;

    private SearchRow start, end, intersects;
    private Cursor cursor;
    private Column inColumn;
    private int inListIndex;
    private Value[] inList;
    private ResultInterface inResult;

    public IndexCursor(TableFilter filter) {
        this.tableFilter = filter;
    }

    public void setIndex(Index index) {
        this.index = index;
        this.table = index.getTable();
        Column[] columns = table.getColumns();
        indexColumns = new IndexColumn[columns.length];
        IndexColumn[] idxCols = index.getIndexColumns();
        if (idxCols != null) {
            for (int i = 0, len = columns.length; i < len; i++) {
                int idx = index.getColumnIndex(columns[i]);
                if (idx >= 0) {
                    indexColumns[i] = idxCols[idx];
                }
            }
        }
    }

    /**
     * Prepare this index cursor to make a lookup in index.
     *
     * @param s Session.
     * @param indexConditions Index conditions.
     */
    public void prepare(Session s, ArrayList<IndexCondition> indexConditions) {
        alwaysFalse = false;
        start = end = null;
        inList = null;
        inColumn = null;
        inResult = null;
        intersects = null;
        for (IndexCondition condition : indexConditions) {
            if (condition.isAlwaysFalse()) {
                alwaysFalse = true;
                break;
            }
            // If index can perform only full table scan do not try to use it for regular
            // lookups, each such lookup will perform an own table scan.
            if (index.isFindUsingFullTableScan()) {
                continue;
            }
            Column column = condition.getColumn();
            if (condition.getCompareType() == Comparison.IN_LIST) {
                if (start == null && end == null) {
                    if (canUseIndexForIn(column)) {
                        System.out.println(">xxx> optimized branch");
                        this.inColumn = column;
                        inList = condition.getCurrentValueList(s);
                        inListIndex = 0;
                    }
                    else
                        System.out.println(">xxx> not optimized :(");
                }
            } else if (condition.getCompareType() == Comparison.IN_QUERY) {
                if (start == null && end == null) {
                    if (canUseIndexForIn(column)) {
                        this.inColumn = column;
                        inResult = condition.getCurrentResult();
                    }
                }
            } else {
                Value v = condition.getCurrentValue(s);
                boolean isStart = condition.isStart();
                boolean isEnd = condition.isEnd();
                boolean isIntersects = condition.isSpatialIntersects();
                int columnId = column.getColumnId();
                if (columnId != SearchRow.ROWID_INDEX) {
                    IndexColumn idxCol = indexColumns[columnId];
                    if (idxCol != null && (idxCol.sortType & SortOrder.DESCENDING) != 0) {
                        // if the index column is sorted the other way, we swap
                        // end and start NULLS_FIRST / NULLS_LAST is not a
                        // problem, as nulls never match anyway
                        boolean temp = isStart;
                        isStart = isEnd;
                        isEnd = temp;
                    }
                }
                if (isStart) {
                    start = table.getSearchRow(start, columnId, v, true);
                }
                if (isEnd) {
                    end = table.getSearchRow(end, columnId, v, false);
                }
                if (isIntersects) {
                    intersects = getSpatialSearchRow(intersects, columnId, v);
                }
                // An X=? condition will produce less rows than
                // an X IN(..) condition, unless the X IN condition can use the index.
                if ((isStart || isEnd) && !canUseIndexFor(inColumn)) {
                    inColumn = null;
                    inList = null;

                    if (inResult != null)
                        inResult.close();

                    inResult = null;
                }
            }
        }
        if (inColumn != null) {
            start = table.getTemplateRow();
        }
    }

    /**
     * Re-evaluate the start and end values of the index search for rows.
     *
     * @param s the session
     * @param indexConditions the index conditions
     */
    public void find(Session s, ArrayList<IndexCondition> indexConditions) {
        System.out.println(">xxx> prepare=" + indexConditions.size());
        prepare(s, indexConditions);

        if (inColumn != null) {
            return;
        }

        if (!alwaysFalse) {
            if (intersects != null && index instanceof SpatialIndex) {
                cursor = ((SpatialIndex) index).findByGeometry(tableFilter,
                        start, end, intersects);
            } else if (index != null) {
                cursor = index.find(tableFilter, start, end);

                System.out.println(">xxx> find [" + start + " - " + end + "] cur=" + (cursor != null ? cursor.getClass().getSimpleName() : null));
            }
        }
    }

    private boolean canUseIndexForIn(Column column) {
        if (inColumn != null) {
            // only one IN(..) condition can be used at the same time
            System.out.println(">xxx> only one IN(..) condition can be used at the same time");
            return false;
        }
        return canUseIndexFor(column);
    }


    private void dumpStack(String msg) {
        new Exception("<DEBUG><" +
                Thread.currentThread().getName() + '>' + ' ' + msg).printStackTrace(System.err);
    }

    private boolean canUseIndexFor(Column column) {
        // The first column of the index must match this column,
        // or it must be a VIEW index (where the column is null).
        // Multiple IN conditions with views are not supported, see
        // IndexCondition.getMask.
        IndexColumn[] cols = index.getIndexColumns();
        if (cols == null) {
            return true;
        }
        IndexColumn idxCol = cols[0];

        if (idxCol != null)
            System.out.println(">xxx> " + Objects.equals(idxCol.column, column) + " " + idxCol.column + " vs " + column + ", cols " + Arrays.toString(cols));

        // todo
        boolean res = idxCol == null || idxCol.column == column || (cols.length > 1 && cols[1].column == column);

        System.out.println(">xxx> canUseIndexFor = " + res + ", inColumn=" +inColumn);

        return res;
    }

    private SearchRow getSpatialSearchRow(SearchRow row, int columnId, Value v) {
        if (row == null) {
            row = table.getTemplateRow();
        } else if (row.getValue(columnId) != null) {
            // if an object needs to overlap with both a and b,
            // then it needs to overlap with the union of a and b
            // (not the intersection)
            ValueGeometry vg = (ValueGeometry) row.getValue(columnId).
                    convertTo(Value.GEOMETRY);
            v = ((ValueGeometry) v.convertTo(Value.GEOMETRY)).
                    getEnvelopeUnion(vg);
        }
        if (columnId == SearchRow.ROWID_INDEX) {
            row.setKey(v.getLong());
        } else {
            row.setValue(columnId, v);
        }
        return row;
    }

    /**
     * Check if the result is empty for sure.
     *
     * @return true if it is
     */
    public boolean isAlwaysFalse() {
        return alwaysFalse;
    }

    /**
     * Get start search row.
     *
     * @return search row
     */
    public SearchRow getStart() {
        return start;
    }

    /**
     * Get end search row.
     *
     * @return search row
     */
    public SearchRow getEnd() {
        return end;
    }

    @Override
    public Row get() {
        if (cursor == null) {
            return null;
        }
        return cursor.get();
    }

    @Override
    public SearchRow getSearchRow() {
        return cursor.getSearchRow();
    }

    @Override
    public boolean next() {
        while (true) {
            if (cursor == null) {
                nextCursor();
                if (cursor == null) {
                    return false;
                }
            }
            if (cursor.next()) {
                return true;
            }
            cursor = null;
        }
    }

    private void nextCursor() {
        if (inList != null) {
            System.out.println(">xxx> inList handling");
            while (inListIndex < inList.length) {
                Value v = inList[inListIndex++];
                if (v != ValueNull.INSTANCE) {
                    find(v);
                    break;
                }
            }
        } else if (inResult != null) {
            while (inResult.next()) {
                Value v = inResult.currentRow()[0];
                if (v != ValueNull.INSTANCE) {
                    find(v);
                    break;
                }
            }
        }
    }

    private void find(Value v) {
        start.setValue(inColumn.getColumnId(), inColumn.convert(v));

        // todo
        ArrayList<IndexCondition> idxConds = tableFilter.getIndexConditions();

        IndexCondition cond = tableFilter.getIndexConditions().get(1);

        for (IndexCondition c : idxConds) {
            if (c.getCompareType() == Comparison.EQUAL && cond.getExpression().isConstant())
                start.setValue(cond.getColumn().getColumnId(), cond.getExpression().getValue(null));
        }

        cursor = index.find(tableFilter, start, start);
    }

    @Override
    public boolean previous() {
        throw DbException.throwInternalError(toString());
    }

    @Override
    public void close() throws Exception {
        if (inResult != null)
            inResult.close();

        if (cursor instanceof AutoCloseable)
            ((AutoCloseable)cursor).close();
    }
}
