/*
 * EXELIXI
 *
 * Copyright (C) 2017 Endri Bezati, EPFL SCI-STI-MM
 *
 * This file is part of EXELIXI.
 *
 * EXELIXI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EXELIXI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EXELIXI. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or
 * an Eclipse library), containing parts covered by the terms of the
 * Eclipse Public License (EPL), the licensors of this Program grant you
 * additional permission to convey the resulting work.  Corresponding Source
 * for a non-source form of such a combination shall include the source code
 * for the parts of Eclipse libraries used as well as that of the covered work.
 *
 */

package xyz.exelixi.utils;

/**
 * A simple util class for retrieving the name and the board of Xilinx
 * device/board.
 *
 * @author Endri Bezati
 */
public class XilinxDevices {

    private String name;

    private String board;

    private String fpga;

    public XilinxDevices(String name) {
        this.name = name;
        if (name.equals("AC701")) {
            board = "xilinx.com:ac701:part0:1.2";
            fpga = "xc7a200tfbg676-2";
        } else if (name.equals("KC705")) {
            board = "xilinx.com:kc705:part0:1.2";
            fpga = "xc7k325tffg900-2";
        } else if (name.equals("KCU105")) {
            board = "xilinx.com:kcu105:part0:1.1";
            fpga = "xcku040-ffva1156-2-e";
        } else if (name.equals("VC707")) {
            board = "xilinx.com:vc707:part0:1.2";
            fpga = "xc7vx485tffg1761-2";
        } else if (name.equals("VC709")) {
            board = "xilinx.com:vc709:part0:1.7";
            fpga = "xc7vx690tffg1761-2";
        } else if (name.equals("VCU 108")) {
            board = "xilinx.com:vc709:part0:1.7";
            fpga = "xc7vx690tffg1761-2";
        } else if (name.equals("ZC702")) {
            board = "xilinx.com:zc702:part0:1.2";
            fpga = "xc7z020clg484-1";
        } else if (name.equals("ZC706")) {
            board = "xilinx.com:zc706:part0:1.2";
            fpga = "xc7z045ffg900-2";
        } else if (name.equals("Zedboard")) {
            board = "em.avnet.com:zed:part0:1.3 ";
            fpga = "xc7z020clg484-1";
        } else if (name.equals("OZ745")) {
            board = "";
            fpga = "xc7z045ffg900-3";
        } else if (name.equals("ZCU102")) {
            board = "xilinx.com:zcu102:part0:1.1";
            fpga = "xczu9eg-ffvb1156-1-i-es1";
        }

    }

    public String getFpga() {
        return fpga;
    }

    public String getBoard() {
        return board;
    }

    public String getName() {
        return name;
    }

    public boolean isZynq() {
        if (name.equals("ZC7027")) {
            return true;
        } else if (name.equals("ZC706")) {
            return true;
        } else if (name.equals("Zedboard")) {
            return true;
        } else if (name.equals("OZ745")) {
            return true;
        } else if (name.equals("ZCU102")) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isZynqUltrascale() {
        return name.equals("ZCU102");
    }

    public int getDDRLength() {
        if (name.equals("ZC702")) {
            return 0x3FF00000;
        } else if (name.equals("ZC706")) {
            return 0x3FF00000;
        } else if (name.equals("Zedboard")) {
            return 0x1FF00000;
        } else if (name.equals("OZ745")) {
            return 0x3FF00000;
        } else if (name.equals("ZCU102")) {
            return 0x3FF00000;
        }
        return 0;
    }

}

