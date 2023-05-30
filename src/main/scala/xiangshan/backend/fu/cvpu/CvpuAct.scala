package xiangshan.backend.fu.cvpu

import chipsalliance.rocketchip.config.Parameters
import chisel3._
import chisel3.util._
import utils._
import xiangshan._

// act.relu
class ActReluDataModule(sew: Int)(implicit p: Parameters) extends XSModule {
  val io = IO(new Bundle() {
    val src_data   = Input(UInt(XLEN.W))
    val zero_point = Input(UInt(16.W))
    val res_data   = Output(UInt(XLEN.W))
  })

  val num = XLEN / sew
  val src_data_vec = Wire(Vec(num, SInt(sew.W)))
  val res_data_vec = Wire(Vec(num, UInt(sew.W)))
  val zero_point = io.zero_point(sew-1, 0).asSInt

  for(i <- 0 until num) {
    src_data_vec(i) := io.src_data(sew*(i+1)-1, sew*i).asSInt
    res_data_vec(i) := Mux(src_data_vec(i) >= zero_point, src_data_vec(i), zero_point).asUInt
  }

  io.res_data := res_data_vec.reduce{ (a, b) => Cat(b, a) }
}

class ActReluModule(implicit p: Parameters) extends XSModule {
  val io = IO(new Bundle() {
    val src_data  = Input(UInt(XLEN.W))
    val zero_data = Input(UInt(16.W))
    val sew       = Input(UInt(2.W))
    val res_data  = Output(UInt(XLEN.W))
  })

  val actrelu_module_0 = Module(new ActReluDataModule(2))  //sew=2
  val actrelu_module_1 = Module(new ActReluDataModule(4))  //sew=4
  val actrelu_module_2 = Module(new ActReluDataModule(8))  //sew=8
  val actrelu_module_3 = Module(new ActReluDataModule(16)) //sew=16

  val actrelu_module = VecInit(Seq(actrelu_module_0.io, actrelu_module_1.io, actrelu_module_2.io, actrelu_module_3.io))
  for(i <- 0 until 4) {
    actrelu_module(i).src_data   := io.src_data
    actrelu_module(i).zero_point := io.zero_data
  }

  io.res_data := LookupTree(io.sew, List(
    CvpuElementFormat.e2  -> actrelu_module_0.io.res_data,
    CvpuElementFormat.e4  -> actrelu_module_1.io.res_data,
    CvpuElementFormat.e8  -> actrelu_module_2.io.res_data,
    CvpuElementFormat.e16 -> actrelu_module_3.io.res_data
  ))
}

// act.relu6
class ActRelu6DataModule(sew: Int)(implicit p: Parameters) extends XSModule {
  val io = IO(new Bundle() {
    val src_data   = Input(UInt(XLEN.W))
    val zero_point = Input(UInt(16.W))
    val q6         = Input(UInt(16.W))
    val res_data   = Output(UInt(XLEN.W))
  })

  val num = XLEN / sew
  val src_data_vec = Wire(Vec(num, SInt(sew.W)))
  val res_data_vec = Wire(Vec(num, UInt(sew.W)))
  val zero_point = io.zero_point(sew-1, 0).asSInt
  val q6 = io.q6(sew-1, 0).asSInt

  for(i <- 0 until num) {
    src_data_vec(i) := io.src_data(sew*(i+1)-1, sew*i).asSInt
    val relu_res = Mux(src_data_vec(i) >= zero_point, src_data_vec(i), zero_point)
    res_data_vec(i) := Mux(relu_res <= q6, relu_res, q6).asUInt
  }

  io.res_data := res_data_vec.reduce{ (a, b) => Cat(b, a) }
}

class ActRelu6Module(implicit p: Parameters) extends XSModule {
  val io = IO(new Bundle() {
    val src_data  = Input(UInt(XLEN.W))
    val zero_data = Input(UInt(16.W))
    val q6        = Input(UInt(16.W))
    val sew       = Input(UInt(2.W))
    val res_data  = Output(UInt(XLEN.W))
  })

  val actrelu6_module_0 = Module(new ActRelu6DataModule(2))  //sew=2
  val actrelu6_module_1 = Module(new ActRelu6DataModule(4))  //sew=4
  val actrelu6_module_2 = Module(new ActRelu6DataModule(8))  //sew=8
  val actrelu6_module_3 = Module(new ActRelu6DataModule(16)) //sew=16

  val actrelu6_module = VecInit(Seq(actrelu6_module_0.io, actrelu6_module_1.io, actrelu6_module_2.io, actrelu6_module_3.io))
  for(i <- 0 until 4) {
    actrelu6_module(i).src_data   := io.src_data
    actrelu6_module(i).zero_point := io.zero_data
    actrelu6_module(i).q6         := io.q6
  }

  io.res_data := LookupTree(io.sew, List(
    CvpuElementFormat.e2  -> actrelu6_module_0.io.res_data,
    CvpuElementFormat.e4  -> actrelu6_module_1.io.res_data,
    CvpuElementFormat.e8  -> actrelu6_module_2.io.res_data,
    CvpuElementFormat.e16 -> actrelu6_module_3.io.res_data
  ))
}
