/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nexial.core.tools

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Options
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS
import org.nexial.commons.utils.IOFilePathFilter
import org.nexial.commons.utils.RegexUtils
import org.nexial.core.NexialConst.ExitStatus.RC_BAD_CLI_ARGS
import org.nexial.core.NexialConst.TEMP
import org.nexial.core.tools.CliConst.OPT_VERBOSE
import org.nexial.core.tools.CliUtils.getCommandLine
import org.nexial.core.tools.ProjectToolUtils.log
import org.nexial.core.tools.inspector.InspectorConst.exit
import org.nexial.core.utils.ConsoleUtils
import java.io.File
import java.io.File.separator
import java.util.*

object TempCleanUp {
    private const val filePattern = "(_nexial_)?[a-zA-Z]{5}"
    private val cal = Calendar.getInstance()

    @JvmStatic
    fun main(args: Array<String>) {
        val cmd = deriveCommandLine(args)
        val verbose = cmd.hasOption(OPT_VERBOSE.opt)
        cleanTempFiles(verbose)
    }

    private fun deriveCommandLine(args: Array<String>): CommandLine {
        val cmdOptions = Options()
        cmdOptions.addOption(OPT_VERBOSE)

        val programExt = if (IS_OS_WINDOWS) ".cmd" else ".sh"
        val cmd = getCommandLine("nexial-clean$programExt", args, cmdOptions)
        if (cmd == null) {
            ConsoleUtils.error("unable to proceed... exiting")
            exit(RC_BAD_CLI_ARGS)
        }

        return cmd
    }

    fun cleanTempFiles(verbose: Boolean) {
        IOFilePathFilter(true).filterFiles(TEMP).filter {
            // filter directory with pattern `(_nexial_)?[a-zA-Z]{5}`
            RegexUtils.isExact(StringUtils.substringBetween(it, TEMP, separator), filePattern) &&
            isModifiedOneDayBefore(Date(File(it).lastModified()))
        }.forEach {
            val file = File(TEMP + StringUtils.substringBetween(it, TEMP, separator))
            if (verbose) log("delete", file.absolutePath)
            if (!FileUtils.deleteQuietly(file)) ConsoleUtils.error("unable to delete directory: ${file.absolutePath}")
        }
    }

    private fun isModifiedOneDayBefore(modifiedDate: Date): Boolean {
        cal.add(Calendar.HOUR, -24)
        return modifiedDate.before(cal.time)
    }
}