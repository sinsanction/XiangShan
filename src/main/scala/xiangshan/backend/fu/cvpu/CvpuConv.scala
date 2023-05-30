package xiangshan.backend.fu.cvpu

import chipsalliance.rocketchip.config.Parameters
import chisel3._
import chisel3.util._
import utils._
import xiangshan._

// conv
class ConvDataModule(sew: Int)(implicit p: Parameters) extends XSModule {
  val io = IO(new Bundle() {
    val src_data  = Input(UInt(XLEN.W))
    val ker_data  = Input(UInt(XLEN.W))
    val prev_data = Input(UInt(32.W))
    val res_data  = Output(UInt(32.W))
  })

  val num = XLEN / sew
  val src_data_vec = Wire(Vec(num, SInt(sew.W)))
  val ker_data_vec = Wire(Vec(num, SInt(sew.W)))
  val mul_vec = Wire(Vec(num, SInt((2*sew).W)))

  for(i <- 0 until num) {
    src_data_vec(i) := io.src_data(sew*(i+1)-1, sew*i).asSInt
    ker_data_vec(i) := io.ker_data(sew*(i+1)-1, sew*i).asSInt
    mul_vec(i) := src_data_vec(i) * ker_data_vec(i)
  }

  val res_data = Wire(SInt(32.W))
  res_data := io.prev_data.asSInt + mul_vec.reduce(_ +& _)

  io.res_data := res_data.asUInt
}

class ConvModule(implicit p: Parameters) extends XSModule {
  val io = IO(new Bundle() {
    val src_data  = Input(UInt(XLEN.W))
    val ker_data  = Input(UInt(XLEN.W))
    val prev_data = Input(UInt(32.W))
    val sew       = Input(UInt(2.W))
    val res_data  = Output(UInt(XLEN.W))
  })

  val conv_module_0 = Module(new ConvDataModule(2))  //sew=2
  val conv_module_1 = Module(new ConvDataModule(4))  //sew=4
  val conv_module_2 = Module(new ConvDataModule(8))  //sew=8
  val conv_module_3 = Module(new ConvDataModule(16)) //sew=16

  val conv_module = VecInit(Seq(conv_module_0.io, conv_module_1.io, conv_module_2.io, conv_module_3.io))
  for(i <- 0 until 4) {
    conv_module(i).src_data  := io.src_data
    conv_module(i).ker_data  := io.ker_data
    conv_module(i).prev_data := io.prev_data
  }

  io.res_data := ZeroExt(LookupTree(io.sew, List(
    CvpuElementFormat.e2  -> conv_module_0.io.res_data,
    CvpuElementFormat.e4  -> conv_module_1.io.res_data,
    CvpuElementFormat.e8  -> conv_module_2.io.res_data,
    CvpuElementFormat.e16 -> conv_module_3.io.res_data
  )), XLEN)
}
