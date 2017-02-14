#!/bin/bash
#=================================================================================
# DEFAULT VALUES
#=================================================================================

MARCH=emulator

#=================================================================================
# PARSING
#=================================================================================
for i in "$@"
do
case $i in
    -march=*)
    MARCH="${i#*=}"
    shift # past argument=value
    ;;
    -h*|-help*)
    HELP=YES
    shift # past argument with no value
    ;;
    *)
    # unknown option
    ;;
esac
done

# print help if required
if [ -n "$HELP" ]; then 
echo "$0 [-march=<value>]"
echo ""
echo "if no march is specified, the AOCL emulator will be used"
exit;
fi


#=================================================================================
# LAUNCH AOCL
#=================================================================================

mkdir -p bin/emu
aoc -march=$MARCH device/device.cl -o bin/emu/device.aocx