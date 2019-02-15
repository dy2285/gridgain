/*
 *                   GridGain Community Edition Licensing
 *                   Copyright 2019 GridGain Systems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License") modified with Commons Clause
 * Restriction; you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * Commons Clause Restriction
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including without
 * limitation fees for hosting or consulting/ support services related to the Software), a product or
 * service whose value derives, entirely or substantially, from the functionality of the Software.
 * Any license notice or attribution required by the License must also include this Commons Clause
 * License Condition notice.
 *
 * For purposes of the clause above, the “Licensor” is Copyright 2019 GridGain Systems, Inc.,
 * the “License” is the Apache License, Version 2.0, and the Software is the GridGain Community
 * Edition software provided with this notice.
 */

import {Subject, Observable, combineLatest, merge} from 'rxjs';
import {pluck, tap, publishReplay, refCount, distinctUntilChanged, switchMap, map} from 'rxjs/operators';

import get from 'lodash/get';

import hasIndexTemplate from './hasIndex.template.pug';
import keyCellTemplate from './keyCell.template.pug';
import valueCellTemplate from './valueCell.template.pug';

import {removeClusterItems, advancedSaveModel} from 'app/components/page-configure/store/actionCreators';

import {default as ConfigSelectors} from 'app/components/page-configure/store/selectors';
import {default as ConfigureState} from 'app/components/page-configure/services/ConfigureState';
import {default as Models} from 'app/services/Models';

export default class PageConfigureAdvancedModels {
    static $inject = ['ConfigSelectors', 'ConfigureState', '$uiRouter', 'Models', '$state', 'configSelectionManager'];

    /**
     * @param {ConfigSelectors} ConfigSelectors
     * @param {ConfigureState} ConfigureState
     * @param {Models} Models
     * @param {uirouter.UIRouter} $uiRouter
     * @param {uirouter.StateService} $state
     */
    constructor(ConfigSelectors, ConfigureState, $uiRouter, Models, $state, configSelectionManager) {
        this.$state = $state;
        this.$uiRouter = $uiRouter;
        this.configSelectionManager = configSelectionManager;
        this.ConfigSelectors = ConfigSelectors;
        this.ConfigureState = ConfigureState;
        this.Models = Models;
    }
    $onDestroy() {
        this.subscription.unsubscribe();
        this.visibleRows$.complete();
        this.selectedRows$.complete();
    }
    $onInit() {
        /** @type {Subject<Array<ig.config.model.ShortDomainModel>>} */
        this.visibleRows$ = new Subject();

        /** @type {Subject<Array<ig.config.model.ShortDomainModel>>} */
        this.selectedRows$ = new Subject();

        /** @type {Array<uiGrid.IColumnDefOf<ig.config.model.ShortDomainModel>>} */
        this.columnDefs = [
            {
                name: 'hasIndex',
                displayName: 'Indexed',
                field: 'hasIndex',
                type: 'boolean',
                enableFiltering: true,
                visible: true,
                multiselectFilterOptions: [{value: true, label: 'Yes'}, {value: false, label: 'No'}],
                width: 100,
                cellTemplate: hasIndexTemplate
            },
            {
                name: 'keyType',
                displayName: 'Key type',
                field: 'keyType',
                enableHiding: false,
                filter: {
                    placeholder: 'Filter by key type…'
                },
                cellTemplate: keyCellTemplate,
                minWidth: 165
            },
            {
                name: 'valueType',
                displayName: 'Value type',
                field: 'valueType',
                enableHiding: false,
                filter: {
                    placeholder: 'Filter by value type…'
                },
                sort: {direction: 'asc', priority: 0},
                cellTemplate: valueCellTemplate,
                minWidth: 165
            }
        ];

        /** @type {Observable<string>} */
        this.itemID$ = this.$uiRouter.globals.params$.pipe(pluck('modelID'));

        /** @type {Observable<Array<ig.config.model.ShortDomainModel>>} */
        this.shortItems$ = this.ConfigureState.state$.pipe(
            this.ConfigSelectors.selectCurrentShortModels,
            tap((shortModels = []) => {
                const value = shortModels.every((m) => m.hasIndex);
                this.columnDefs[0].visible = !value;
            }),
            publishReplay(1),
            refCount()
        );

        this.shortCaches$ = this.ConfigureState.state$.pipe(this.ConfigSelectors.selectCurrentShortCaches);

        /** @type {Observable<ig.config.model.DomainModel>} */
        this.originalItem$ = this.itemID$.pipe(
            distinctUntilChanged(),
            switchMap((id) => {
                return this.ConfigureState.state$.pipe(this.ConfigSelectors.selectModelToEdit(id));
            }),
            distinctUntilChanged(),
            publishReplay(1),
            refCount()
        );

        this.isNew$ = this.itemID$.pipe(map((id) => id === 'new'));

        this.itemEditTitle$ = combineLatest(this.isNew$, this.originalItem$, (isNew, item) => {
            return `${isNew ? 'Create' : 'Edit'} model ${!isNew && get(item, 'valueType') ? `‘${get(item, 'valueType')}’` : ''}`;
        });

        this.selectionManager = this.configSelectionManager({
            itemID$: this.itemID$,
            selectedItemRows$: this.selectedRows$,
            visibleRows$: this.visibleRows$,
            loadedItems$: this.shortItems$
        });

        this.tableActions$ = this.selectionManager.selectedItemIDs$.pipe(map((selectedItems) => [
            {
                action: 'Clone',
                click: () => this.clone(selectedItems),
                available: false
            },
            {
                action: 'Delete',
                click: () => {
                    this.remove(selectedItems);
                },
                available: true
            }
        ]));

        this.subscription = merge(
            this.originalItem$,
            this.selectionManager.editGoes$.pipe(tap((id) => this.edit(id))),
            this.selectionManager.editLeaves$.pipe(tap((options) => this.$state.go('base.configuration.edit.advanced.models', null, options)))
        ).subscribe();
    }

    edit(modelID) {
        this.$state.go('base.configuration.edit.advanced.models.model', {modelID});
    }

    save({model, download}) {
        this.ConfigureState.dispatchAction(advancedSaveModel(model, download));
    }

    /**
     * @param {Array<string>} itemIDs
     */
    remove(itemIDs) {
        this.ConfigureState.dispatchAction(
            removeClusterItems(this.$uiRouter.globals.params.clusterID, 'models', itemIDs, true, true)
        );
    }
}
