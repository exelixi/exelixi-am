#!/bin/bash
#=================================================================================
# PARSING
#=================================================================================
for i in "$@"
do
case $i in
    -march=*)
    MARCH="${i#*=}"
    shift
    ;;
    -board=*)
    BOARD="${i#*=}"
    shift
    ;;
    -h*|-help*)
    HELP=YES
    shift
    ;;
    *)
    # unknown option
    ;;
esac
done

# print help if required
if [ -n "$HELP" ]; then
echo "$00 [-march=<value>] [-board=<value>]"
echo ""
echo "   if no march is specified, the AOCL emulator will be used"
echo "   usage example: $0 -board=de1soc_sharedonly"
exit;
fi


#=================================================================================
# BUILD CONFIGURATION
#=================================================================================

if [ -z "$BOARD" ] && [ -z "$MARCH" ]; then
CONFIG="-march=emulator"
BINPATH=bin/emu
else

	if [ -n "$BOARD" ]; then
		CONFIG=$CONFIG" --board $BOARD"
	fi

	if [ -n "$MARCH" ]; then
		CONFIG=$CONFIG" -march=$MARCH"
	fi

	BINPATH=bin/arm

fi

#=================================================================================
# LAUNCH AOCL
#=================================================================================

echo "configuration: -o $BINPATH/device.aocx device/device.cl $CONFIG"
echo "start........: $(date -R)"

mkdir -p $BINPATH
aoc -o $BINPATH/device.aocx device/device.cl $CONFIG

echo "end..........: $(date -R)"