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

'use strict';

const _ = require('lodash');
const logger = require('morgan');
const cookieParser = require('cookie-parser');
const bodyParser = require('body-parser');
const session = require('express-session');
const connectMongo = require('connect-mongo');
const passport = require('passport');
const passportSocketIo = require('passport.socketio');
const mongoSanitize = require('express-mongo-sanitize');

// Fire me up!

/**
 * Module for configuration express and websocket server.
 */
module.exports = {
    implements: 'configure',
    inject: ['settings', 'mongo', 'middlewares:*']
};

module.exports.factory = function(settings, mongo, apis) {
    const _sessionStore = new (connectMongo(session))({mongooseConnection: mongo.connection});

    return {
        express: (app) => {
            app.use(logger('dev', {
                skip: (req, res) => res.statusCode < 400
            }));

            _.forEach(apis, (api) => app.use(api));

            app.use(bodyParser.json({limit: '50mb'}));
            app.use(bodyParser.urlencoded({limit: '50mb', extended: true}));


            app.use(mongoSanitize({replaceWith: '_'}));

            app.use(session({
                secret: settings.sessionSecret,
                resave: false,
                saveUninitialized: true,
                unset: 'destroy',
                cookie: {
                    expires: new Date(Date.now() + settings.cookieTTL),
                    maxAge: settings.cookieTTL
                },
                store: _sessionStore
            }));

            app.use(passport.initialize());
            app.use(passport.session());

            passport.serializeUser((user, done) => done(null, user._id));

            passport.deserializeUser((id, done) => {
                if (mongo.ObjectId.isValid(id))
                    return mongo.Account.findById(id, done);

                // Invalidates the existing login session.
                done(null, false);
            });

            passport.use(mongo.Account.createStrategy());
        },
        socketio: (io) => {
            const _onAuthorizeSuccess = (data, accept) => accept();

            const _onAuthorizeFail = (data, message, error, accept) => {
                if (error)
                    accept(new Error(message));

                return accept(new Error(message));
            };

            io.use(passportSocketIo.authorize({
                cookieParser,
                key: 'connect.sid', // the name of the cookie where express/connect stores its session_id
                secret: settings.sessionSecret, // the session_secret to parse the cookie
                store: _sessionStore, // we NEED to use a sessionstore. no memorystore please
                success: _onAuthorizeSuccess, // *optional* callback on success - read more below
                fail: _onAuthorizeFail // *optional* callback on fail/error - read more below
            }));
        }
    };
};
