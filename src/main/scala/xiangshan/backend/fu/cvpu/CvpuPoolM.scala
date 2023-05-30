package xiangshan.backend.fu.cvpu

import chipsalliance.rocketchip.config.Parameters
import chisel3._
import chisel3.util._
import utils._
import xiangshan._

// pool.m.max
class PoolMMaxDataModule(sew: Int)(implicit p: Parameters) extends XSModule {
  val io = IO(new Bundle() {
    val src_data  = Input(UInt(XLEN.W))
    val res_data  = Output(UInt(16.W))
  })

  val num = XLEN / sew
  val res_num = num / 4
  val src_data_vec = Wire(Vec(res_num , Vec(4, SInt(sew.W))))
  val res_data_vec = Wire(Vec(res_num , UInt(sew.W)))

  for(i <- 0 until res_num) {
    for(j <- 0 until 4) {
      src_data_vec(i)(j) := io.src_data(sew*(i*4+j+1)-1, sew*(i*4+j)).asSInt
    }
  }

  for(i <- 0 until res_num) {
    res_data_vec(i) := ParallelMaxSInt(src_data_vec(i)).asUInt
  }

  io.res_data := res_data_vec.reduce{ (a, b) => Cat(b, a) }
}

class PoolMMaxModule(implicit p: Parameters) extends XSModule {
  val io = IO(new Bundle() {
    val src_data  = Input(UInt(XLEN.W))
    val sew       = Input(UInt(2.W))
    val window    = Input(UInt(2.W))
    val res_data  = Output(UInt(XLEN.W))
  })

  //assert(io.window === 2.U, "don't support window != 2")

  val poolmmax_module_0 = Module(new PoolMMaxDataModule(2))  //sew=2
  val poolmmax_module_1 = Module(new PoolMMaxDataModule(4))  //sew=4
  val poolmmax_module_2 = Module(new PoolMMaxDataModule(8))  //sew=8
  val poolmmax_module_3 = Module(new PoolMMaxDataModule(16)) //sew=16

  val poolmmax_module = VecInit(Seq(poolmmax_module_0.io, poolmmax_module_1.io, poolmmax_module_2.io, poolmmax_module_3.io))
  for(i <- 0 until 4) {
    poolmmax_module(i).src_data  := io.src_data
  }

  io.res_data := ZeroExt(LookupTree(io.sew, List(
    CvpuElementFormat.e2  -> poolmmax_module_0.io.res_data,
    CvpuElementFormat.e4  -> poolmmax_module_1.io.res_data,
    CvpuElementFormat.e8  -> poolmmax_module_2.io.res_data,
    CvpuElementFormat.e16 -> poolmmax_module_3.io.res_data
  )), XLEN)
}

// pool.m.avg
class PoolMAvgDataModule(sew: Int)(implicit p: Parameters) extends XSModule {
  val io = IO(new Bundle() {
    val src_data  = Input(UInt(XLEN.W))
    val res_data  = Output(UInt(16.W))
  })

  val num = XLEN / sew
  val res_num = num / 4
  val src_data_vec = Wire(Vec(res_num , Vec(4, SInt(sew.W))))
  val res_data_vec = Wire(Vec(res_num , UInt(sew.W)))

  for(i <- 0 until res_num) {
    for(j <- 0 until 4) {
      src_data_vec(i)(j) := io.src_data(sew*(i*4+j+1)-1, sew*(i*4+j)).asSInt
    }
  }

  for(i <- 0 until res_num) {
    val sum = ParallelSingedExpandingAdd(src_data_vec(i)).asUInt
    val sign = sum(sum.getWidth-1)
    res_data_vec(i) := Mux(sign, ~((~sum + 1.U) >> 2) + 1.U, sum >> 2)
  }

  io.res_data := res_data_vec.reduce{ (a, b) => Cat(b, a) }
}

class PoolMAvgModule(implicit p: Parameters) extends XSModule {
  val io = IO(new Bundle() {
    val src_data  = Input(UInt(XLEN.W))
    val sew       = Input(UInt(2.W))
    val window    = Input(UInt(2.W))
    val res_data  = Output(UInt(XLEN.W))
  })

  //assert(io.window === 2.U, "don't support window != 2")

  val poolmavg_module_0 = Module(new PoolMAvgDataModule(2))  //sew=2
  val poolmavg_module_1 = Module(new PoolMAvgDataModule(4))  //sew=4
  val poolmavg_module_2 = Module(new PoolMAvgDataModule(8))  //sew=8
  val poolmavg_module_3 = Module(new PoolMAvgDataModule(16)) //sew=16

  val poolmavg_module = VecInit(Seq(poolmavg_module_0.io, poolmavg_module_1.io, poolmavg_module_2.io, poolmavg_module_3.io))
  for(i <- 0 until 4) {
    poolmavg_module(i).src_data  := io.src_data
  }

  io.res_data := ZeroExt(LookupTree(io.sew, List(
    CvpuElementFormat.e2  -> poolmavg_module_0.io.res_data,
    CvpuElementFormat.e4  -> poolmavg_module_1.io.res_data,
    CvpuElementFormat.e8  -> poolmavg_module_2.io.res_data,
    CvpuElementFormat.e16 -> poolmavg_module_3.io.res_data
  )), XLEN)
}
