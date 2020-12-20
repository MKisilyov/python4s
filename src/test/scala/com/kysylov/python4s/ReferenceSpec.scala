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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ReferenceSpec extends AnyFlatSpec with Matchers {

  "A reference wrapper" should "increment and decrement the reference counter" in {
    val sys = Python.importModule("sys")

    val pyObject = PythonObject("test")
    sys.getrefcount(pyObject).toInt shouldEqual 1

    val newRef = PythonReference.borrow(pyObject.reference.pointer)
    sys.getrefcount(pyObject).toInt shouldEqual 2

    newRef.foreach(_.close())
    PythonReference.reclaim()
    sys.getrefcount(pyObject).toInt shouldEqual 1
  }

}
