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

#include <time.h>
#include <vector>

#include <windows.h>

#include <ignite/common/platform_utils.h>

// Original code is suggested by MSDN at
// https://docs.microsoft.com/en-us/windows/win32/sysinfo/converting-a-time-t-value-to-a-file-time
// Modified to fit larger time values
void TimetToFileTime(time_t tt, LPFILETIME pft)
{
    ULARGE_INTEGER uli;
    uli.QuadPart = tt * 10000000 + 116444736000000000LL;

    pft->dwLowDateTime = uli.LowPart;
    pft->dwHighDateTime = uli.HighPart;
}

namespace ignite
{
    namespace common
    {
        time_t IgniteTimeGm(const tm& time)
        {
            tm tmc = time;

            return _mkgmtime(&tmc);
        }

        time_t IgniteTimeLocal(const tm& time)
        {
            tm tmc = time;

            return mktime(&tmc);
        }

        bool IgniteGmTime(time_t in, tm& out)
        {
            FILETIME fileTime;
            TimetToFileTime(in, &fileTime);

            SYSTEMTIME localTime;
            if (!FileTimeToSystemTime(&fileTime, &localTime))
                return false;

            SYSTEMTIME systemTime;
            if (!SystemTimeToTzSpecificLocalTime(NULL, &localTime, &systemTime))
                return false;

            out.tm_year = systemTime.wYear - 1900;
            out.tm_mon = systemTime.wMonth - 1;
            out.tm_mday = systemTime.wDay;
            out.tm_hour = systemTime.wHour;
            out.tm_min = systemTime.wMinute;
            out.tm_sec = systemTime.wSecond;

            return true;
        }

        bool IgniteLocalTime(time_t in, tm& out)
        {            FILETIME fileTime;
            TimetToFileTime(in, &fileTime);

            SYSTEMTIME localTime;
            if (!FileTimeToSystemTime(&fileTime, &localTime))
                return false;

            out.tm_year = localTime.wYear - 1900;
            out.tm_mon = localTime.wMonth - 1;
            out.tm_mday = localTime.wDay;
            out.tm_hour = localTime.wHour;
            out.tm_min = localTime.wMinute;
            out.tm_sec = localTime.wSecond;

            return true;
        }

        std::string GetEnv(const std::string& name)
        {
            static const std::string empty;

            return GetEnv(name, empty);
        }

        std::string GetEnv(const std::string& name, const std::string& dflt)
        {
            char res[32767];

            DWORD envRes = GetEnvironmentVariableA(name.c_str(), res, sizeof(res) / sizeof(res[0]));

            if (envRes == 0 || envRes > sizeof(res))
                return dflt;

            return std::string(res, static_cast<size_t>(envRes));
        }

        bool FileExists(const std::string& path)
        {
            WIN32_FIND_DATAA findres;

            HANDLE hnd = FindFirstFileA(path.c_str(), &findres);

            if (hnd == INVALID_HANDLE_VALUE)
                return false;

            FindClose(hnd);

            return true;
        }

        bool IsValidDirectory(const std::string& path)
        {
            if (path.empty())
                return false;

            DWORD attrs = GetFileAttributesA(path.c_str());

            return attrs != INVALID_FILE_ATTRIBUTES && (attrs & FILE_ATTRIBUTE_DIRECTORY) != 0;
        }

        bool DeletePath(const std::string& path)
        {
            std::vector<TCHAR> path0(path.begin(), path.end());
            path0.push_back('\0');
            path0.push_back('\0');

            SHFILEOPSTRUCT fileop;
            fileop.hwnd = NULL;
            fileop.wFunc = FO_DELETE;
            fileop.pFrom = &path0[0];
            fileop.pTo = NULL;
            fileop.fFlags = FOF_NOCONFIRMATION | FOF_SILENT;

            fileop.fAnyOperationsAborted = FALSE;
            fileop.lpszProgressTitle = NULL;
            fileop.hNameMappings = NULL;

            int ret = SHFileOperation(&fileop);

            return ret == 0;
        }

        StdCharOutStream& Fs(StdCharOutStream& ostr)
        {
            ostr.put('\\');
            return ostr;
        }

        StdCharOutStream& Dle(StdCharOutStream& ostr)
        {
            static const char expansion[] = ".dll";

            ostr.write(expansion, sizeof(expansion) - 1);

            return ostr;
        }

        IGNITE_IMPORT_EXPORT unsigned GetRandSeed()
        {
            return static_cast<unsigned>(GetTickCount() ^ GetCurrentProcessId());
        }
    }
}
