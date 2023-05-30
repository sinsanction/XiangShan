package xiangshan.backend.fu.cvpu

import chipsalliance.rocketchip.config.Parameters
import chisel3._
import chisel3.util._
import utils._
import xiangshan._

// add.n
class AddNDataModule(sew: Int)(implicit p: Parameters) extends XSModule {
  val io = IO(new Bundle() {
    val src_a_data = Input(UInt(XLEN.W))
    val src_b_data = Input(UInt(XLEN.W))
    val zero_point = Input(UInt(16.W))
    val res_data   = Output(UInt(XLEN.W))
  })

  val num = XLEN / sew
  val src_a_data_vec = Wire(Vec(num, SInt(sew.W)))
  val src_b_data_vec = Wire(Vec(num, SInt(sew.W)))
  val res_data_vec = Wire(Vec(num, UInt(sew.W)))
  val zero_point = io.zero_point(sew-1, 0).asSInt

  for(i <- 0 until num) {
    src_a_data_vec(i) := io.src_a_data(sew*(i+1)-1, sew*i).asSInt
    src_b_data_vec(i) := io.src_b_data(sew*(i+1)-1, sew*i).asSInt
    val add_res = src_a_data_vec(i) +& src_b_data_vec(i) -& zero_point
    val add_res_clip = Mux(add_res < (-(1 << (sew - 1))).S, (-(1 << (sew - 1))).S, Mux(add_res > ((1 << (sew - 1)) - 1).S, ((1 << (sew - 1)) - 1).S, add_res(sew-1, 0).asSInt))
    res_data_vec(i) := add_res_clip.asUInt
  }

  io.res_data := res_data_vec.reduce{ (a, b) => Cat(b, a) }
}

class AddNModule(implicit p: Parameters) extends XSModule {
  val io = IO(new Bundle() {
    val src_a_data = Input(UInt(XLEN.W))
    val src_b_data = Input(UInt(XLEN.W))
    val zero_data  = Input(UInt(16.W))
    val sew        = Input(UInt(2.W))
    val res_data   = Output(UInt(XLEN.W))
  })

  val addn_module_0 = Module(new AddNDataModule(2))  //sew=2
  val addn_module_1 = Module(new AddNDataModule(4))  //sew=4
  val addn_module_2 = Module(new AddNDataModule(8))  //sew=8
  val addn_module_3 = Module(new AddNDataModule(16)) //sew=16

  val addn_module = VecInit(Seq(addn_module_0.io, addn_module_1.io, addn_module_2.io, addn_module_3.io))
  for(i <- 0 until 4) {
    addn_module(i).src_a_data := io.src_a_data
    addn_module(i).src_b_data := io.src_b_data
    addn_module(i).zero_point := io.zero_data
  }

  io.res_data := LookupTree(io.sew, List(
    CvpuElementFormat.e2  -> addn_module_0.io.res_data,
    CvpuElementFormat.e4  -> addn_module_1.io.res_data,
    CvpuElementFormat.e8  -> addn_module_2.io.res_data,
    CvpuElementFormat.e16 -> addn_module_3.io.res_data
  ))
}

// add.relu
class AddReluDataModule(sew: Int)(implicit p: Parameters) extends XSModule {
  val io = IO(new Bundle() {
    val src_a_data = Input(UInt(XLEN.W))
    val src_b_data = Input(UInt(XLEN.W))
    val zero_point = Input(UInt(16.W))
    val res_data   = Output(UInt(XLEN.W))
  })

  val num = XLEN / sew
  val src_a_data_vec = Wire(Vec(num, SInt(sew.W)))
  val src_b_data_vec = Wire(Vec(num, SInt(sew.W)))
  val res_data_vec = Wire(Vec(num, UInt(sew.W)))
  val zero_point = io.zero_point(sew-1, 0).asSInt

  for(i <- 0 until num) {
    src_a_data_vec(i) := io.src_a_data(sew*(i+1)-1, sew*i).asSInt
    src_b_data_vec(i) := io.src_b_data(sew*(i+1)-1, sew*i).asSInt
    val add_res = src_a_data_vec(i) +& src_b_data_vec(i) -& zero_point
    val add_res_clip = Mux(add_res < zero_point, zero_point, Mux(add_res > ((1 << (sew - 1)) - 1).S, ((1 << (sew - 1)) - 1).S, add_res(sew-1, 0).asSInt))
    res_data_vec(i) := add_res_clip.asUInt
  }

  io.res_data := res_data_vec.reduce{ (a, b) => Cat(b, a) }
}

class AddReluModule(implicit p: Parameters) extends XSModule {
  val io = IO(new Bundle() {
    val src_a_data = Input(UInt(XLEN.W))
    val src_b_data = Input(UInt(XLEN.W))
    val zero_data  = Input(UInt(16.W))
    val sew        = Input(UInt(2.W))
    val res_data   = Output(UInt(XLEN.W))
  })

  val addrelu_module_0 = Module(new AddReluDataModule(2))  //sew=2
  val addrelu_module_1 = Module(new AddReluDataModule(4))  //sew=4
  val addrelu_module_2 = Module(new AddReluDataModule(8))  //sew=8
  val addrelu_module_3 = Module(new AddReluDataModule(16)) //sew=16

  val addrelu_module = VecInit(Seq(addrelu_module_0.io, addrelu_module_1.io, addrelu_module_2.io, addrelu_module_3.io))
  for(i <- 0 until 4) {
    addrelu_module(i).src_a_data := io.src_a_data
    addrelu_module(i).src_b_data := io.src_b_data
    addrelu_module(i).zero_point := io.zero_data
  }

  io.res_data := LookupTree(io.sew, List(
    CvpuElementFormat.e2  -> addrelu_module_0.io.res_data,
    CvpuElementFormat.e4  -> addrelu_module_1.io.res_data,
    CvpuElementFormat.e8  -> addrelu_module_2.io.res_data,
    CvpuElementFormat.e16 -> addrelu_module_3.io.res_data
  ))
}
