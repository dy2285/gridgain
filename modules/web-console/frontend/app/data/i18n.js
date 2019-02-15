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

export default {
    '/agent/start': 'Agent start',
    '/agent/download': 'Agent download',
    '/configuration/overview': 'Cluster configurations',
    '/configuration/new': 'Cluster configuration create',
    '/configuration/new/basic': 'Basic cluster configuration create',
    '/configuration/new/advanced/cluster': 'Advanced cluster configuration create',
    '/configuration/download': 'Download project',
    'configuration/import/model': 'Import cluster models',
    '/demo/resume': 'Demo resume',
    '/demo/reset': 'Demo reset',
    '/queries/execute': 'Query execute',
    '/queries/explain': 'Query explain',
    '/queries/scan': 'Scan',
    '/queries/add/query': 'Add query',
    '/queries/add/scan': 'Add scan',
    '/queries/demo': 'SQL demo',
    '/queries/notebook/': 'Query notebook',
    '/settings/profile': 'User profile',
    '/settings/admin': 'Admin panel',
    '/logout': 'Logout',

    'base.configuration.overview': 'Cluster configurations',
    'base.configuration.edit.basic': 'Basic cluster configuration edit',
    'base.configuration.edit.advanced.cluster': 'Advanced cluster configuration edit',
    'base.configuration.edit.advanced.caches': 'Advanced cluster caches',
    'base.configuration.edit.advanced.caches.cache': 'Advanced cluster cache edit',
    'base.configuration.edit.advanced.models': 'Advanced cluster models',
    'base.configuration.edit.advanced.models.model': 'Advanced cluster model edit',
    'base.configuration.edit.advanced.igfs': 'Advanced cluster IGFSs',
    'base.configuration.edit.advanced.igfs.igfs': 'Advanced cluster IGFS edit',
    'base.settings.admin': 'Admin panel',
    'base.settings.profile': 'User profile',
    'base.sql.notebook': 'Query notebook',
    'base.sql.tabs.notebooks-list': 'Query notebooks',

    // app/components/page-signin/template.pug
    'app.components.page-signin.m1': 'Sign In',
    'app.components.page-signin.m2': 'Email:',
    'app.components.page-signin.m3': 'Input email',
    'app.components.page-signin.m4': 'Password:',
    'app.components.page-signin.m5': 'Input password',
    'app.components.page-signin.m6': 'Forgot password?',
    'app.components.page-signin.m7': 'Sign In',
    'app.components.page-signin.m8': 'Don\'t have an account? #[a(ui-sref=\'signup\') Get started]',

    // app/components/page-signin/run.js
    'app.components.page-signin.m9': 'Sign In',


    // app/components/page-queries/template.tpl.pug
    'app.components.page-queries.m1': 'Show data in tabular form',
    'app.components.page-queries.m2': 'Show bar chart<br/>By default first column - X values, second column - Y values<br/>In case of one column it will be treated as Y values',
    'app.components.page-queries.m3': 'Show pie chart<br/>By default first column - pie labels, second column - pie values<br/>In case of one column it will be treated as pie values',
    'app.components.page-queries.m4': 'Show line chart<br/>By default first column - X values, second column - Y values<br/>In case of one column it will be treated as Y values',
    'app.components.page-queries.m5': 'Show area chart<br/>By default first column - X values, second column - Y values<br/>In case of one column it will be treated as Y values',
    'app.components.page-queries.m6': 'Click to show chart settings dialog',
    'app.components.page-queries.m7': 'Chart settings',
    'app.components.page-queries.m8': 'Show',
    'app.components.page-queries.m9': 'min',
    'app.components.page-queries.m10': 'Duration: #[b {{paragraph.duration | duration}}]',
    'app.components.page-queries.m11': 'NodeID8: #[b {{paragraph.resNodeId | id8}}]',
    'app.components.page-queries.m12': 'Rename notebook',
    'app.components.page-queries.m13': 'Remove notebook',
    'app.components.page-queries.m14': 'Save notebook name',
    'app.components.page-queries.m15': 'Scroll to query',
    'app.components.page-queries.m16': 'Add query',
    'app.components.page-queries.m17': 'Add scan',
    'app.components.page-queries.m18': 'Failed to load notebook',
    'app.components.page-queries.m19': 'Notebook not accessible any more. Leave notebooks or open another notebook.',
    'app.components.page-queries.m20': 'Leave notebooks',
    'app.components.page-queries.m21': 'Rename query',
    'app.components.page-queries.m22': 'Rename query',
    'app.components.page-queries.m23': 'Remove query',
    'app.components.page-queries.m24': 'Save query name',
    'app.components.page-queries.m25': 'Configure periodical execution of last successfully executed query',
    'app.components.page-queries.m26': 'Refresh rate:',
    'app.components.page-queries.m27': 'Max number of rows to show in query result as one page',
    'app.components.page-queries.m28': 'Page size:',
    'app.components.page-queries.m29': 'Limit query max results to specified number of pages',
    'app.components.page-queries.m30': 'Max pages:',
    'app.components.page-queries.m31': 'Non-collocated joins is a special mode that allow to join data across cluster without collocation.<br/>Nested joins are not supported for now.<br/><b>NOTE</b>: In some cases it may consume more heap memory or may take a long time than collocated joins.',
    'app.components.page-queries.m32': 'Allow non-collocated joins',
    'app.components.page-queries.m33': 'Enforce join order of tables in the query.<br/>If <b>set</b>, then query optimizer will not reorder tables within join.<br/><b>NOTE:</b> It is not recommended to enable this property unless you have verified that indexes are not selected in optimal order.',
    'app.components.page-queries.m34': 'Enforce join order',
    'app.components.page-queries.m35': 'By default Ignite attempts to fetch the whole query result set to memory and send it to the client.<br/>For small and medium result sets this provides optimal performance and minimize duration of internal database locks, thus increasing concurrency.<br/>If result set is too big to fit in available memory this could lead to excessive GC pauses and even OutOfMemoryError.<br/>Use this flag as a hint for Ignite to fetch result set lazily, thus minimizing memory consumption at the cost of moderate performance hit.',
    'app.components.page-queries.m36': 'Lazy result set',
    'app.components.page-queries.m37': 'Execute',
    'app.components.page-queries.m38': 'Execute on selected node',
    'app.components.page-queries.m39': '{{queryTooltip(paragraph, "explain query")}}',
    'app.components.page-queries.m40': 'Explain',
    'app.components.page-queries.m41': 'Page: #[b {{paragraph.page}}]',
    'app.components.page-queries.m42': 'Results so far: #[b {{paragraph.rows.length + paragraph.total}}]',
    'app.components.page-queries.m43': 'Duration: #[b {{paragraph.duration | duration}}]',
    'app.components.page-queries.m44': 'NodeID8: #[b {{paragraph.resNodeId | id8}}]',
    'app.components.page-queries.m45': '{{ queryTooltip(paragraph, "export query results") }}',
    'app.components.page-queries.m46': 'Export',
    'app.components.page-queries.m47': 'Export',
    'app.components.page-queries.m48': 'Export all',
    'app.components.page-queries.m49': 'Copy current result page to clipboard',
    'app.components.page-queries.m50': 'Copy to clipboard',
    'app.components.page-queries.m51': 'Page: #[b {{paragraph.pa',
    'app.components.page-queries.m52': 'Results so far: #[b {{paragraph.rows.length + paragraph.total}}]',
    'app.components.page-queries.m53': 'Duration: #[b {{paragraph.duration | duration}}]',
    'app.components.page-queries.m54': 'NodeID8: #[b {{paragraph.resNodeId | id8}}]',
    'app.components.page-queries.m55': 'Export',
    'app.components.page-queries.m56': 'Export',
    'app.components.page-queries.m57': 'Export all',
    'app.components.page-queries.m58': 'Copy current result page to clipboard',
    'app.components.page-queries.m59': 'Copy to clipboard',
    'app.components.page-queries.m60': 'Cannot display chart. Please configure axis using #[b Chart settings]',
    'app.components.page-queries.m61': 'Cannot display chart. Result set must contain Java build-in type columns. Please change query and execute it again.',
    'app.components.page-queries.m62': 'Pie chart does not support \'TIME_LINE\' column for X-axis. Please use another column for X-axis or switch to another chart.',
    'app.components.page-queries.m63': 'Charts do not support #[b Explain] and #[b Scan] query',
    'app.components.page-queries.m64': 'Cache:',
    'app.components.page-queries.m65': 'Choose cache',
    'app.components.page-queries.m66': 'Filter:',
    'app.components.page-queries.m67': 'Enter filter',
    'app.components.page-queries.m68': 'Select this checkbox for case sensitive search',
    'app.components.page-queries.m69': 'Max number of rows to show in query result as one page',
    'app.components.page-queries.m70': 'Page size:',
    'app.components.page-queries.m71': 'Scan',
    'app.components.page-queries.m72': 'Scan on selected node',
    'app.components.page-queries.m73': 'Error: {{paragraph.error.message}}',
    'app.components.page-queries.m74': 'Result set is empty. Duration: #[b {{paragraph.duration | duration}}]',
    'app.components.page-queries.m75': 'Showing results for scan of #[b {{ paragraph.queryArgs.cacheName | defaultName }}]',
    'app.components.page-queries.m76': '&nbsp; with filter: #[b {{ paragraph.queryArgs.filter }}]',
    'app.components.page-queries.m77': '&nbsp; on node: #[b {{ paragraph.queryArgs.localNid | limitTo:8 }}]',
    'app.components.page-queries.m78': 'Next',
    'app.components.page-queries.m79': 'Caches:',
    'app.components.page-queries.m80': 'Click to show cache types metadata dialog',
    'app.components.page-queries.m81': 'Filter caches...',
    'app.components.page-queries.m82': 'Use selected cache as default schema name.<br/>This will allow to execute query on specified cache without specify schema name.<br/><b>NOTE:</b> In future version of Ignite this feature will be removed.',
    'app.components.page-queries.m83': 'Use selected cache as default schema name',
    'app.components.page-queries.m84': 'Wrong caches filter',
    'app.components.page-queries.m85': 'No caches',
    'app.components.page-queries.m86': 'Error: {{paragraph.error.message}}',
    'app.components.page-queries.m87': 'Show more',
    'app.components.page-queries.m88': 'Show query',
    'app.components.page-queries.m89': 'Next',
    'app.components.page-queries.m90': 'Queries',
    'app.components.page-queries.m91': 'With query notebook you can',
    'app.components.page-queries.m92': 'Create any number of queries',
    'app.components.page-queries.m93': 'Execute and explain SQL queries',
    'app.components.page-queries.m94': 'Execute scan queries',
    'app.components.page-queries.m95': 'View data in tabular form and as charts',
    'app.components.page-queries.m96': 'Examples:',
    // app/components/page-queries/Notebook.service.js
    'app.components.page-queries.m97': 'Are you sure you want to remove notebook: "${notebook.name}"?',
    // app/components/page-queries/Notebook.data.js
    'app.components.page-queries.m98': 'SQL demo',
    'app.components.page-queries.m99': 'Query with refresh rate',
    'app.components.page-queries.m100': 'Simple query',
    'app.components.page-queries.m101': 'Query with aggregates',
    'app.components.page-queries.m102': 'Failed to load notebook.',
    'app.components.page-queries.m103': 'Removing "${notebook.name}" notebook is not supported.',
    // app/components/page-queries/index.js
    'app.components.page-queries.m104': 'Query notebook',
    'app.components.page-queries.m105': 'SQL demo',
    // app/components/page-queries/controller.js
    'app.components.page-queries.m106': 'Internal cluster error',
    'app.components.page-queries.m107': 'Unlimited',
    'app.components.page-queries.m108': 'Demo grid is starting. Please wait...',
    'app.components.page-queries.m109': 'Loading query notebook screen...',
    'app.components.page-queries.m110': 'seconds',
    'app.components.page-queries.m111': 's',
    'app.components.page-queries.m112': 'minutes',
    'app.components.page-queries.m113': 'm',
    'app.components.page-queries.m114': 'hours',
    'app.components.page-queries.m115': 'h',
    'app.components.page-queries.m116': 'Leave Queries',
    'app.components.page-queries.m117': 'Query ${sz === 0 ? "" : sz}',
    'app.components.page-queries.m118': 'Scan ${sz === 0 ? "" : sz}',
    'app.components.page-queries.m119': 'Are you sure you want to remove query: "${paragraph.name}"?',
    'app.components.page-queries.m120': 'Waiting for server response',
    'app.components.page-queries.m121': 'Input text to ${action}',
    'app.components.page-queries.m122': 'Waiting for server response',
    'app.components.page-queries.m123': 'Select cache to export scan results',
    'app.components.page-queries.m124': 'SCAN query',
    'app.components.page-queries.m125': 'SCAN query for cache: <b>${maskCacheName(paragraph.queryArgs.cacheName, true)}</b>',
    'app.components.page-queries.m126': 'SCAN query for cache: <b>${maskCacheName(paragraph.queryArgs.cacheName, true)}</b> with filter: <b>${filter}</b>',
    'app.components.page-queries.m127': 'Explain query',
    'app.components.page-queries.m128': 'SQL query',
    'app.components.page-queries.m129': 'Duration: ${$filter(\'duration\')(paragraph.duration)}.',
    'app.components.page-queries.m130': 'Node ID8: ${_.id8(paragraph.resNodeId)}',
    'app.components.page-queries.m131': 'Error details',

    // app/components/page-queries/services/queries-navbar.js
    'app.components.page-queries.services.queries-navbar.m1': 'Queries',
    'app.components.page-queries.services.queries-navbar.m2': 'Create new notebook',

    // app/components/page-queries/services/create-query-dialog/template.pug
    'app.components.page-queries.services.create-query-dialog.m1': 'New query notebook',
    'app.components.page-queries.services.create-query-dialog.m2': 'Name:&nbsp;',
    'app.components.page-queries.services.create-query-dialog.m3': 'Cancel',
    'app.components.page-queries.services.create-query-dialog.m4': 'Create',

    // app/components/page-profile/controller.js
    'app.components.page-profile.m1': 'Are you sure you want to change security token?',
    'app.components.page-profile.m2': 'Profile saved.',
    'app.components.page-profile.m3': 'Failed to save profile: ',
    // app/components/page-profile/index.js
    'app.components.page-profile.m4': 'User profile',
    // app/components/page-profile/template.pug
    'app.components.page-profile.m5': 'User profile',
    'app.components.page-profile.m6': 'First name:',
    'app.components.page-profile.m7': 'Input first name',
    'app.components.page-profile.m8': 'Last name:',
    'app.components.page-profile.m9': 'Input last name',
    'app.components.page-profile.m10': 'Email:',
    'app.components.page-profile.m11': 'Input email',
    'app.components.page-profile.m12': 'Phone:',
    'app.components.page-profile.m13': 'Input phone (ex.: +15417543010)',
    'app.components.page-profile.m14': 'Country:',
    'app.components.page-profile.m15': 'Choose your country',
    'app.components.page-profile.m16': 'Company:',
    'app.components.page-profile.m17': 'Input company name',
    'app.components.page-profile.m18': 'Cancel security token changing...',
    'app.components.page-profile.m19': 'Show security token...',
    'app.components.page-profile.m20': 'Security token:',
    'app.components.page-profile.m21': 'No security token. Regenerate please.',
    'app.components.page-profile.m22': 'Generate random security token',
    'app.components.page-profile.m23': 'Copy security token to clipboard',
    'app.components.page-profile.m24': 'The security token is used for authorization of web agent',
    'app.components.page-profile.m25': 'Cancel password changing...',
    'app.components.page-profile.m26': 'Change password...',
    'app.components.page-profile.m27': 'New password:',
    'app.components.page-profile.m28': 'New password',
    'app.components.page-profile.m29': 'Confirm password:',
    'app.components.page-profile.m30': 'Confirm new password',
    'app.components.page-profile.m31': 'Cancel',
    'app.components.page-profile.m32': 'Save Changes',

    // app/modules/navbar/userbar.directive.js
    'app/modules/navbar/userbar.m1': 'Profile',
    'app/modules/navbar/userbar.m2': 'Getting started',
    'app/modules/navbar/userbar.m3': 'Admin panel',
    'app/modules/navbar/userbar.m4': 'Log out',

    // app/components/page-forgot-password/run.js
    'app.components.page-forgot-password.m1': 'Forgot Password',
    // app/components/page-forgot-password/template.pug
    'app.components.page-forgot-password.m2': 'Forgot password?',
    'app.components.page-forgot-password.m3': 'Enter the email address for your account & we\'ll email you a link to reset your password.',
    'app.components.page-forgot-password.m4': 'Email:',
    'app.components.page-forgot-password.m5': 'Input email',
    'app.components.page-forgot-password.m6': 'Back to sign in',
    'app.components.page-forgot-password.m7': 'Send it to me',

    // app/components/page-landing/template.pug
    'app.components.page-landing.m1': 'Sign In',
    'app.components.page-landing.m2': 'Web Console',
    'app.components.page-landing.m3': 'An Interactive Configuration Wizard and Management Tool for Apache™ Ignite®',
    'app.components.page-landing.m4': 'It provides an interactive configuration wizard which helps you create and download configuration files and code snippets for your Apache Ignite projects. Additionally, the tool allows you to automatically load SQL metadata from any RDBMS, run SQL queries on your in-memory cache, and view execution plans, in-memory schema, and streaming charts.',
    'app.components.page-landing.m5': 'Sign Up',
    'app.components.page-landing.m6': 'The Web Console allows you to:',
    'app.components.page-landing.m7': 'Configure Apache Ignite clusters and caches',
    'app.components.page-landing.m8': 'The Web Console configuration wizard takes you through a step-by-step process that helps you define all the required configuration parameters. The system then generates a ready-to-use project with all the required config files.',
    'app.components.page-landing.m9': 'Run free-form SQL queries on #[br] Apache Ignite caches',
    'app.components.page-landing.m10': 'By connecting the Web Console to your Apache Ignite cluster, you can execute SQL queries on your in-memory cache. You can also view the execution plan, in-memory schema, and streaming charts for your cluster.',
    'app.components.page-landing.m11': 'Import database schemas from #[br] virtually any RDBMS',
    'app.components.page-landing.m12': 'To speed the creation of your configuration files, the Web Console allows you to automatically import the database schema from virtually any RDBMS including Oracle, SAP, MySQL, PostgreSQL, and many more.',
    'app.components.page-landing.m13': 'Manage the Web Console users',
    'app.components.page-landing.m14': 'The Web Console allows you to have accounts with different roles.',
    'app.components.page-landing.m15': 'Get Started',

    // app/components/page-signup/template.pug
    'app.components.page-signup.m1': 'Don\'t Have An Account?',
    'app.components.page-signup.m2': 'Email:',
    'app.components.page-signup.m3': 'Input email',
    'app.components.page-signup.m4': 'Password:',
    'app.components.page-signup.m5': 'Input password',
    'app.components.page-signup.m6': 'Confirm:',
    'app.components.page-signup.m7': 'Confirm password',
    'app.components.page-signup.m8': 'First name:',
    'app.components.page-signup.m9': 'Input first name',
    'app.components.page-signup.m10': 'Last name:',
    'app.components.page-signup.m11': 'Input last name',
    'app.components.page-signup.m12': 'Phone:',
    'app.components.page-signup.m13': 'Input phone (ex.: +15417543010)',
    'app.components.page-signup.m14': 'Country:',
    'app.components.page-signup.m15': 'Choose your country',
    'app.components.page-signup.m16': 'Company:',
    'app.components.page-signup.m17': 'Input company name',
    'app.components.page-signup.m18': 'Sign Up',
    'app.components.page-signup.m19': 'Already have an account? #[a(ui-sref=\'signin\') Sign in here]',

    // app/components/password-visibility/toggle-button.component.js
    'app.components.password-visibility.m1': 'Hide password',
    'app.components.password-visibility.m2': 'Show password'
};
