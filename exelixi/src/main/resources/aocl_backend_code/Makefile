# You must configure ALTERAOCLSDKROOT to point the root directory of the Intel(R) FPGA SDK for OpenCL(TM)
# software installation.
# See http://www.altera.com/literature/hb/opencl-sdk/aocl_getting_started.pdf 
# for more information on installing and configuring the Intel(R) FPGA SDK for OpenCL(TM).

# initialize the enviroment.....: source /opt/intelFPGA/16.1/hld/init_opencl.sh
# run the host on emulation mode: CL_CONTEXT_EMULATOR_DEVICE_ALTERA=1 bin/host

#DEBUG=1
ifeq ($(VERBOSE),1)
ECHO :=
else
ECHO := @
endif

# Where is the Intel(R) FPGA SDK for OpenCL(TM) software?
ifeq ($(wildcard $(ALTERAOCLSDKROOT)),)
$(error Set ALTERAOCLSDKROOT to the root directory of the Intel(R) FPGA SDK for OpenCL(TM) software installation)
endif
ifeq ($(wildcard $(ALTERAOCLSDKROOT)/host/include/CL/opencl.h),)
$(error Set ALTERAOCLSDKROOT to the root directory of the Intel(R) FPGA SDK for OpenCL(TM) software installation.)
endif

# Compilation flags
ifeq ($(DEBUG),1)
CXXFLAGS += -g
else
CXXFLAGS += -O2
endif

# Compiler
CXX := g++

# Directories
INC_DIRS := host/include
LIB_DIRS :=

# Files
INCS := $(wildcard )
SRCS := $(wildcard host/src/*.cpp)



# EMU
EMU_AOCL_COMPILE_CONFIG := $(shell aocl compile-config )
EMU_AOCL_LINK_CONFIG := $(shell aocl link-config )
EMU_LIBS := rt pthread
EMU_TARGET := host
EMU_TARGET_DIR := bin/emu


# arm cross compiler
CROSS-COMPILE = arm-linux-gnueabihf-
ARM_AOCL_COMPILE_CONFIG=$(shell aocl compile-config --arm) -Ihost/include
ARM_AOCL_LINK_CONFIG=$(shell aocl link-config --arm)  -lacl_emulator_kernel_rt
ARM_TARGET     := host
ARM_TARGET_DIR := bin/arm

# Make it all: use the emulator by default
all : $(EMU_TARGET_DIR)/$(EMU_TARGET)

# make emu
emu : $(EMU_TARGET_DIR)/$(EMU_TARGET)

$(EMU_TARGET_DIR)/$(EMU_TARGET) : Makefile $(SRCS) $(INCS) $(EMU_TARGET_DIR)
	$(ECHO)$(CXX) $(CPPFLAGS) $(CXXFLAGS) -fPIC $(foreach D,$(INC_DIRS),-I$D) \
			$(EMU_AOCL_COMPILE_CONFIG) $(SRCS) $(EMU_AOCL_LINK_CONFIG) \
			$(foreach D,$(LIB_DIRS),-L$D) \
			$(foreach L,$(EMU_LIBS),-l$L) \
			-o $(EMU_TARGET_DIR)/$(EMU_TARGET)

$(EMU_TARGET_DIR) :
	$(ECHO)mkdir $(EMU_TARGET_DIR)

$(ARM_TARGET_DIR) :
	$(ECHO)mkdir $(ARM_TARGET_DIR)

# make clean
clean :
	$(ECHO)rm -f $(EMU_TARGET_DIR)/$(EMU_TARGET)
	$(ECHO)rm -f $(ARM_TARGET_DIR)/$(ARM_TARGET)

# make arm
arm : $(ARM_TARGET_DIR)/$(ARM_TARGET)

$(ARM_TARGET_DIR)/$(ARM_TARGET) : Makefile $(SRCS) $(INCS) $(ARM_TARGET_DIR)
	$(ECHO)$(CROSS-COMPILE)$(CXX) $(CXXFLAGS) -std=c++11 -lpthread -fpermissive $(SRCS) $(COMMON_FILES) \
		-o $(ARM_TARGET_DIR)/$(ARM_TARGET)  \
		   $(ARM_AOCL_COMPILE_CONFIG) $(ARM_AOCL_LINK_CONFIG)

.PHONY : all emu arm clean