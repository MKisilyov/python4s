/*
 * Copyright 2019 Maksym Kysylov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kysylov.python4s

import jnr.ffi.LibraryLoader

import scala.language.dynamics
import scala.sys.process._

object Python extends Dynamic {
  // locate executable and shared library
  val Array(executable, libraryDirectory, libraryName) =
    s"""${sys.env.getOrElse("PYTHON", "python")} -c "
       |import os
       |import sys
       |
       |print(sys.executable)
       |print(os.path.join(sys.exec_prefix, 'lib'))
       |print('python{major}.{minor}{abiflags}'.format(
       |  major=sys.version_info.major,
       |  minor=sys.version_info.minor,
       |  abiflags=getattr(sys, 'abiflags', '')
       |))
       |"
    """.stripMargin.!!.split('\n').map(_.trim)

  private[python4s] val libPython = {
    // load shared library
    val library = new PythonLibrary(
      LibraryLoader
        .create(classOf[LibPython])
        .search(libraryDirectory)
        .load(libraryName)
    )

    // program name is required in order to set sys.exec_path
    library.pySetProgramName(executable)

    // initialize interpreter
    library.pyInitializeEx(false)

    // configure runtime
    library.pyRunSimpleString(
      s"""import sys
         |
         |# If there isn’t a script that will be run, the first entry in argv can be an empty string.
         |sys.argv = ['']
         |
         |# Add working directory to module search path.
         |sys.path.insert(0, '')
      """.stripMargin)

    library
  }

  private val builtins = PythonObject(libPython.pyEvalGetBuiltins)

  /**
    * Call a built-in function.
    *
    * @param methodName function name
    * @param args       arguments
    * @return python object
    */
  def applyDynamic(methodName: String)(args: PythonObject*): PythonObject = builtins(methodName)(args: _*)

  /**
    * Call a built-in function with named arguments.
    *
    * @param methodName function name
    * @param args       arguments
    * @return python object
    */
  def applyDynamicNamed(methodName: String)(args: (String, PythonObject)*): PythonObject = builtins(methodName)(
    args.collect { case (key, value) if key.isEmpty => value },
    args.filter { case (key, _) => key.nonEmpty }.toMap
  )

  /**
    * Get a built-in function.
    *
    * @param attributeName function name
    * @return python function
    */
  def selectDynamic(attributeName: String): PythonObject = builtins(attributeName)

  /**
    * Import python module
    *
    * @param name module name
    * @return python module
    */
  def importModule(name: String) = PythonObject(libPython.pyImportImportModule(name))
}
