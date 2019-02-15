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

/* eslint-env node*/

const globby = require('globby');
const lex = require('pug-lexer');
const parse = require('pug-parser');
const walk = require('pug-walk');
const compileAttrs = require('pug-attrs');
const {promisify} = require('util');
const fs = require('fs');
const readFileAsync = promisify(fs.readFile);
const fetch = require('node-fetch');
const ProgressBar = require('progress');
const chalk = require('chalk');
const tsm = require('teamcity-service-messages');
const slash = require('slash');
const appRoot = require('app-root-path').path;

const {argv} = require('yargs')
    .option('directory', {
        alias: 'd',
        describe: 'parent directory to apply glob pattern from',
        default: appRoot
    })
    .option('pugs', {
        alias: 'p',
        describe: 'glob pattern to select templates with',
        default: '{app,views}/**/*.pug'
    })
    .usage('Usage: $0 [options]')
    .example(
        '$0 --pugs="./**/*.pug"',
        `look for all invalid links in all pug files in current dir and it's subdirs`
    )
    .help();

const pugPathToAST = async(pugPath) => Object.assign(parse(lex(await readFileAsync(pugPath, 'utf8'))), {filePath: pugPath});

const findLinks = (acc, ast) => {
    walk(ast, (node) => {
        if (node.attrs) {
            const href = node.attrs.find((attr) => attr.name === 'href');

            if (href) {
                const compiledAttr = compileAttrs([href], {
                    terse: false,
                    format: 'object',
                    runtime() {}
                });

                try {
                    acc.push([JSON.parse(compiledAttr).href, ast.filePath]);
                }
                catch (e) {
                    console.log(ast.filePath, e);
                }
            }
        }
    });
    return acc;
};

const isDocsURL = (url) => url.startsWith('http');

const isInvalidURL = (url) => fetch(url, {redirect: 'manual'}).then((res) => res.status !== 200);

const first = ([value]) => value;

const checkDocLinks = async(pugsGlob, onProgress = () => {}) => {
    const pugs = await globby(pugsGlob);
    const allAST = await Promise.all(pugs.map(pugPathToAST));
    const allLinks = allAST.reduce(findLinks, []).filter((pair) => isDocsURL(first(pair)));

    onProgress(allLinks);

    const tick = (v) => {onProgress(v); return v;};

    const results = await Promise.all(allLinks.map((pair) => {
        return isInvalidURL(first(pair))
        .then((isInvalid) => [...pair, isInvalid])
        .then(tick)
        .catch(tick);
    }));

    const invalidLinks = results.filter(([,, isInvalid]) => isInvalid);

    return {allLinks, invalidLinks};
};

module.exports.checkDocLinks = checkDocLinks;

const specReporter = (allLinks, invalidLinks) => {
    const invalidCount = invalidLinks.length;

    const format = ([link, at], i) => `\n${i + 1}. ${chalk.red(link)} ${chalk.dim('in')} ${chalk.yellow(at)}`;

    console.log(`Total links: ${allLinks.length}`);
    console.log(`Invalid links found: ${invalidCount ? chalk.red(invalidCount) : chalk.green(invalidCount)}`);

    if (invalidCount) console.log(invalidLinks.map(format).join(''));
};

const teamcityReporter = (allLinks, invalidLinks) => {
    const name = 'Checking docs links';
    tsm.testStarted({ name });

    if (invalidLinks.length > 0)
        tsm.testFailed({ name, details: invalidLinks.map(([link, at]) => `\n ${link} in ${at}`).join('') });
    else {
        tsm.testStdOut(`All ${allLinks.length} are correct!`);
        tsm.testFinished({ name: 'Checking docs links' });
    }
};

const main = async() => {
    let bar;
    const updateBar = (value) => {
        if (!bar)
            bar = new ProgressBar('Checking links [:bar] :current/:total', {total: value.length});
        else
            bar.tick();
    };

    const unBackSlashedDirPath = slash(argv.directory);
    const absolutePugGlob = `${unBackSlashedDirPath}/${argv.pugs}`;

    console.log(`Looking for invalid links in ${chalk.cyan(absolutePugGlob)}.`);

    const {allLinks, invalidLinks} = await checkDocLinks(absolutePugGlob, updateBar);

    const reporter = process.env.TEAMCITY ? teamcityReporter : specReporter;
    reporter(allLinks, invalidLinks);
};

main();
