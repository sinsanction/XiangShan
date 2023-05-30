/***************************************************************************************
* Copyright (c) 2020-2021 Institute of Computing Technology, Chinese Academy of Sciences
* Copyright (c) 2020-2021 Peng Cheng Laboratory
*
* XiangShan is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

package xiangshan.backend.decode.isa.cvpdecode

import chisel3.util._

object CVPInstructions {
  def CONV               = BitPat("b0000?????????????000?????0001011")

  def POOLMAX            = BitPat("b0000???00001?????010?????0001011")
  def POOLAVG            = BitPat("b0000???00010?????010?????0001011")

  def ACTRELU            = BitPat("b0000?????????????100?????0001011")
  def ACTRELU6           = BitPat("b0001?????????????100?????0001011")

  def ADDN               = BitPat("b0000?????????????101?????0001011")
  def ADDRELU            = BitPat("b0001?????????????101?????0001011")

  def POOLMMAX           = BitPat("b???????00001?????011?????0001011")
  def POOLMAVG           = BitPat("b???????00010?????011?????0001011")
}
