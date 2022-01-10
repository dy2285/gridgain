/*
 * Copyright 2019 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _IGNITE_NETWORK_SOCKETS
#define _IGNITE_NETWORK_SOCKETS

#include <stdint.h>
#include <string>

#define SOCKET_ERROR (-1)

namespace ignite
{
    namespace network
    {
        namespace sockets
        {
            /** Socket handle type. */
            typedef int SocketHandle;

            /**
             * Get socket error.
             * @return Last socket error.
             */
            int GetLastSocketError();

            /**
             * Get socket error.
             * @param handle Socket handle.
             * @return Last socket error.
             */
            int GetLastSocketError(int handle);

            /**
             * Get socket error message for the error code.
             * @param error Error code.
             * @return Socket error message string.
             */
            std::string GetSocketErrorMessage(int error);

            /**
             * Get last socket error message.
             * @return Last socket error message string.
             */
            std::string GetLastSocketErrorMessage();

            /**
             * Check whether socket operation was interupted.
             * @return @c true if the socket operation was interupted.
             */
            bool IsSocketOperationInterrupted(int errorCode);

            /**
             * Wait on the socket for any event for specified time.
             * This function uses poll to achive timeout functionality
             * for every separate socket operation.
             *
             * @param socket Socket handle.
             * @param timeout Timeout.
             * @param rd Wait for read if @c true, or for write if @c false.
             * @return -errno on error, WaitResult::TIMEOUT on timeout and
             *     WaitResult::SUCCESS on success.
             */
            int WaitOnSocket(SocketHandle socket, int32_t timeout, bool rd);

            /**
             * Try and set socket options.
             *
             * @param socketFd Socket file descriptor.
             * @param bufSize Buffer size.
             * @param noDelay Set no-delay mode.
             * @param outOfBand Set out-of-Band mode.
             * @param keepAlive Keep alive mode.
             */
            void TrySetSocketOptions(int socketFd, int bufSize, bool noDelay, bool outOfBand, bool keepAlive);

            /**
             * Set non blocking mode for socket.
             *
             * @param socketFd Socket file descriptor.
             * @param nonBlocking Non-blocking mode.
             */
            bool SetNonBlockingMode(int socketFd, bool nonBlocking);
        }
    }
}

#endif //_IGNITE_NETWORK_SOCKETS
