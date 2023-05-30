package xiangshan.backend.fu.cvpu

import chipsalliance.rocketchip.config.Parameters
import chisel3._
import chisel3.util._
import utils._
import xiangshan._
import xiangshan.backend.fu._

class CvpuDataIO(implicit p: Parameters) extends XSBundle {
  val src       = Input(Vec(3, UInt(XLEN.W)))

  val opcode    = Input(UInt(5.W))
  val sew       = Input(UInt(2.W))
  val algorithm = Input(UInt(2.W))
  val window    = Input(UInt(2.W))

  val result    = Output(UInt(XLEN.W))
}

class CvpuDataModule(implicit p: Parameters) extends XSModule {
  val io = IO(new CvpuDataIO)

  val conv_Module = Module(new ConvModule)
  conv_Module.io.src_data := io.src(0)
  conv_Module.io.ker_data := io.src(1)
  conv_Module.io.prev_data := io.src(2)(31, 0)
  conv_Module.io.sew := io.sew

  val poolmax_Module = Module(new PoolMaxModule)
  poolmax_Module.io.src_data := io.src(0)
  poolmax_Module.io.prev_data := io.src(1)(15, 0)
  poolmax_Module.io.sew := io.sew

  val poolavg_Module = Module(new PoolAvgModule)
  poolavg_Module.io.src_data := io.src(0)
  poolavg_Module.io.prev_data := io.src(1)(31, 0)
  poolavg_Module.io.sew := io.sew

  val actrelu_Module = Module(new ActReluModule)
  actrelu_Module.io.src_data := io.src(0)
  actrelu_Module.io.zero_data := io.src(1)(15, 0)
  actrelu_Module.io.sew := io.sew

  val actrelu6_Module = Module(new ActRelu6Module)
  actrelu6_Module.io.src_data := io.src(0)
  actrelu6_Module.io.zero_data := io.src(1)(15, 0)
  actrelu6_Module.io.q6 := io.src(1)(31, 16)
  actrelu6_Module.io.sew := io.sew

  val add_Module = Module(new AddNModule)
  add_Module.io.src_a_data := io.src(0)
  add_Module.io.src_b_data := io.src(1)
  add_Module.io.zero_data := io.src(2)(15, 0)
  add_Module.io.sew := io.sew

  val addrelu_Module = Module(new AddReluModule)
  addrelu_Module.io.src_a_data := io.src(0)
  addrelu_Module.io.src_b_data := io.src(1)
  addrelu_Module.io.zero_data := io.src(2)(15, 0)
  addrelu_Module.io.sew := io.sew

  val poolmmax_Module = Module(new PoolMMaxModule)
  poolmmax_Module.io.src_data := io.src(0)
  poolmmax_Module.io.sew := io.sew
  poolmmax_Module.io.window := io.window

  val poolmavg_Module = Module(new PoolMAvgModule)
  poolmavg_Module.io.src_data := io.src(0)
  poolmavg_Module.io.sew := io.sew
  poolmavg_Module.io.window := io.window

  io.result := LookupTree(io.opcode, List(
    CVPUOpType.conv     -> conv_Module.io.res_data,
    CVPUOpType.poolMax  -> poolmax_Module.io.res_data,
    CVPUOpType.poolAvg  -> poolavg_Module.io.res_data,
    CVPUOpType.actRelu  -> actrelu_Module.io.res_data,
    CVPUOpType.actRelu6 -> actrelu6_Module.io.res_data,
    CVPUOpType.add      -> add_Module.io.res_data,
    CVPUOpType.addRelu  -> addrelu_Module.io.res_data,
    CVPUOpType.poolMMax -> poolmmax_Module.io.res_data,
    CVPUOpType.poolMAvg -> poolmavg_Module.io.res_data
  ))
}

class Cvpu(implicit p: Parameters) extends FunctionUnit {

  val uop = io.in.bits.uop

  val cvpuModule = Module(new CvpuDataModule)

  cvpuModule.io.src := io.in.bits.src
  cvpuModule.io.opcode := io.in.bits.uop.ctrl.fuOpType(4, 0)
  cvpuModule.io.sew := io.in.bits.uop.ctrl.cvpu.sew
  cvpuModule.io.algorithm := io.in.bits.uop.ctrl.cvpu.algorithm
  cvpuModule.io.window := io.in.bits.uop.ctrl.cvpu.window

  io.in.ready := io.out.ready
  io.out.valid := io.in.valid
  io.out.bits.uop <> io.in.bits.uop
  io.out.bits.data := cvpuModule.io.result
}
