package oakbot.doclet.cli;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class Arguments {
	private final OptionSet options;

	public Arguments(String[] args) {
		OptionParser parser = new OptionParser();
		parser.accepts("verbose");
		parser.accepts("v");
		parser.accepts("help");

		options = parser.parse(args);
	}
	
	public boolean verbose(){
		return options.has("verbose") || options.has("v");
	}
	
	public boolean help(){
		return options.has("help");
	}
}
