package xyz.exelixi.interp.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import se.lth.cs.tycho.ir.entity.am.ActorMachine;
import xyz.exelixi.interp.BasicActorMachineSimulator;
import xyz.exelixi.interp.BasicChannel;
import xyz.exelixi.interp.BasicEnvironment;
import xyz.exelixi.interp.BasicInterpreter;
import xyz.exelixi.interp.Channel;
import xyz.exelixi.interp.Environment;
import xyz.exelixi.interp.Simulator;
import xyz.exelixi.interp.values.BasicRef;
import xyz.exelixi.interp.values.ConstRef;
import se.lth.cs.tycho.ir.entity.cal.CalActor;
import se.lth.cs.tycho.parsing.cal.CalParser;
import se.lth.cs.tycho.parsing.cal.ParseException;

public class Test {
	public static void main(String[] args) throws FileNotFoundException, ParseException {
		File calFile = new File("../dataflow/examples/Test/My.cal");
		try {
			System.out.println(calFile.getCanonicalPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// File calFile = new
		// File("../dataflow/examples/MPEG4_SP_Decoder/ACPred.cal");

		CalParser parser = new CalParser(new FileInputStream(calFile));
		CalActor calActor = (CalActor) parser.ActorDecl().getEntity();

//		List<Decl> actorArgs = new ArrayList<Decl>();
		// actorArgs.add(varDecl("MAXW_IN_MB", lit(121)));
		// actorArgs.add(varDecl("MB_COORD_SZ", lit(8)));
		// actorArgs.add(varDecl("SAMPLE_SZ", lit(13)));
//		Scope argScope = new Scope(ScopeKind.Persistent, actorArgs);

		ActorMachine actorMachine = BasicActorMachineSimulator.prepareActor(calActor);

//		XMLWriter doc = new XMLWriter(actorMachine);		doc.print();

		Channel channelSource1 = new BasicChannel(3);
		Channel channelSource2 = new BasicChannel(3);
		Channel channelResult = new BasicChannel(30);
		Channel.InputEnd[] sinkChannelInputEnd = { channelResult.getInputEnd() };
		Channel.OutputEnd sinkChannelOutputEnd = channelResult.createOutputEnd();
		Channel.OutputEnd[] sourceChannelOutputEnd = { channelSource1.createOutputEnd(), channelSource2.createOutputEnd() };
		// initial tokens
		channelSource1.getInputEnd().write(ConstRef.of(0));
		channelSource1.getInputEnd().write(ConstRef.of(1));
		channelSource1.getInputEnd().write(ConstRef.of(2));
		channelSource2.getInputEnd().write(ConstRef.of(10));
		channelSource2.getInputEnd().write(ConstRef.of(11));
		channelSource2.getInputEnd().write(ConstRef.of(12));

		int stackSize = 100;
		Environment env = new BasicEnvironment(sinkChannelInputEnd, sourceChannelOutputEnd, actorMachine);
		Simulator runner = new BasicActorMachineSimulator(actorMachine, env, new BasicInterpreter(stackSize));

		while(runner.step()) { ; }
		
		System.out.println("Scopes: ");
		StringBuffer sb = new StringBuffer();
		runner.scopesToString(sb);
		System.out.println(sb);
		System.out.println("Output: ");
		String sep = "";
		int i = 0;
		while (sinkChannelOutputEnd.tokens(i+1)) {
			BasicRef r = new BasicRef();
			sinkChannelOutputEnd.peek(i++, r);
			System.out.print(sep + r);
			sep = ", ";
		}
		System.out.println();
	}
}
