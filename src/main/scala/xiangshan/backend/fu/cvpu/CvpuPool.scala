package xiangshan.backend.fu.cvpu

import chipsalliance.rocketchip.config.Parameters
import chisel3._
import chisel3.util._
import utils._
import xiangshan._

object ParallelMaxSInt {
  def apply[T <: SInt](xs: Seq[T]): T = {
    ParallelOperation(xs, (a:T, b:T) => Mux(a > b, a, b).asTypeOf(xs.head))
  }
}

// pool.max
class PoolMaxDataModule(sew: Int)(implicit p: Parameters) extends XSModule {
  val io = IO(new Bundle() {
    val src_data  = Input(UInt(XLEN.W))
    val prev_data = Input(UInt(16.W))
    val res_data  = Output(UInt(16.W))
  })

  val num = XLEN / sew
  val src_data_vec = Wire(Vec(num, SInt(sew.W)))

  for(i <- 0 until num) {
    src_data_vec(i) := io.src_data(sew*(i+1)-1, sew*i).asSInt
  }

  val max_src_data = ParallelMaxSInt(src_data_vec)

  val res_data = Wire(SInt(sew.W))
  res_data := Mux(io.prev_data(sew-1, 0).asSInt > max_src_data, io.prev_data(sew-1, 0).asSInt, max_src_data)

  io.res_data := SignExt(res_data.asUInt, 16)
}

class PoolMaxModule(implicit p: Parameters) extends XSModule {
  val io = IO(new Bundle() {
    val src_data  = Input(UInt(XLEN.W))
    val prev_data = Input(UInt(16.W))
    val sew       = Input(UInt(2.W))
    val res_data  = Output(UInt(XLEN.W))
  })

  val poolmax_module_0 = Module(new PoolMaxDataModule(2))  //sew=2
  val poolmax_module_1 = Module(new PoolMaxDataModule(4))  //sew=4
  val poolmax_module_2 = Module(new PoolMaxDataModule(8))  //sew=8
  val poolmax_module_3 = Module(new PoolMaxDataModule(16)) //sew=16

  val poolmax_module = VecInit(Seq(poolmax_module_0.io, poolmax_module_1.io, poolmax_module_2.io, poolmax_module_3.io))
  for(i <- 0 until 4) {
    poolmax_module(i).src_data  := io.src_data
    poolmax_module(i).prev_data := io.prev_data
  }

  io.res_data := ZeroExt(LookupTree(io.sew, List(
    CvpuElementFormat.e2  -> poolmax_module_0.io.res_data,
    CvpuElementFormat.e4  -> poolmax_module_1.io.res_data,
    CvpuElementFormat.e8  -> poolmax_module_2.io.res_data,
    CvpuElementFormat.e16 -> poolmax_module_3.io.res_data
  )), XLEN)
}

// pool.avg
class PoolAvgDataModule(sew: Int)(implicit p: Parameters) extends XSModule {
  val io = IO(new Bundle() {
    val src_data  = Input(UInt(XLEN.W))
    val prev_data = Input(UInt(32.W))
    val res_data  = Output(UInt(32.W))
  })

  val num = XLEN / sew
  val src_data_vec = Wire(Vec(num, SInt(sew.W)))

  for(i <- 0 until num) {
    src_data_vec(i) := io.src_data(sew*(i+1)-1, sew*i).asSInt
  }

  val sum_src_data = ParallelSingedExpandingAdd(src_data_vec)

  val res_data = Wire(SInt(32.W))
  res_data := io.prev_data.asSInt + sum_src_data

  io.res_data := res_data.asUInt
}

class PoolAvgModule(implicit p: Parameters) extends XSModule {
  val io = IO(new Bundle() {
    val src_data  = Input(UInt(XLEN.W))
    val prev_data = Input(UInt(32.W))
    val sew       = Input(UInt(2.W))
    val res_data  = Output(UInt(XLEN.W))
  })

  val poolavg_module_0 = Module(new PoolAvgDataModule(2))  //sew=2
  val poolavg_module_1 = Module(new PoolAvgDataModule(4))  //sew=4
  val poolavg_module_2 = Module(new PoolAvgDataModule(8))  //sew=8
  val poolavg_module_3 = Module(new PoolAvgDataModule(16)) //sew=16

  val poolavg_module = VecInit(Seq(poolavg_module_0.io, poolavg_module_1.io, poolavg_module_2.io, poolavg_module_3.io))
  for(i <- 0 until 4) {
    poolavg_module(i).src_data  := io.src_data
    poolavg_module(i).prev_data := io.prev_data
  }

  io.res_data := ZeroExt(LookupTree(io.sew, List(
    CvpuElementFormat.e2  -> poolavg_module_0.io.res_data,
    CvpuElementFormat.e4  -> poolavg_module_1.io.res_data,
    CvpuElementFormat.e8  -> poolavg_module_2.io.res_data,
    CvpuElementFormat.e16 -> poolavg_module_3.io.res_data
  )), XLEN)
}
