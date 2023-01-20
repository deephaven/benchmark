package io.deephaven.benchmark.connect;

import java.util.*;
import java.util.stream.IntStream;

public class ColumnDefs {
	final Random random = new Random(20221130);
	final List<ColumnDef> columns = new ArrayList<>();
	private boolean isFixed = false;
	
	public int getCount() {
		return columns.size();
	}
	
	public boolean isFixed() {
		return isFixed;
	}
	
	public int getMaxValueCount() {
		return columns.stream().mapToInt(c->c.maker.values.size()).max().getAsInt();
	}
	
	public String getQuotedColumns() {
		return columns.stream().map(c->"\"" + c.name + "\"").toList().toString().replaceAll("[\\[\\]]", "");
	}
	
	public Map<String,String> toTypeMap() {
		var typeMap = new LinkedHashMap<String,String>();
		columns.stream().forEach(f->typeMap.put(f.name(), f.type()));
		return typeMap;
	}
	
	public void setFixed() {
		isFixed = true;
	}
	
	public void setRandom() {
		isFixed = false;
	}
	
	public ColumnDefs add(String name, String type, String valueDef) {
		columns.add(new ColumnDef(name, type, valueDef, getMaker(type, valueDef)));
		return this;
	}
	
	public Object nextValue(int columnIndex, long seed) {
		return columns.get(columnIndex).maker().next(seed);
	}
	
	public String describe() {
		String str = "name,type,values\n";
		for(ColumnDef c: columns) {
			str += String.join(",", c.name(), c.type(), c.valueDef()) + "\n";
		}
		return str;
	}

	private Maker getMaker(String type, String valueDef) {
		List<String> values = getParsedValues(valueDef);
		switch(type.toLowerCase()) {
		case "string": return new StringMaker(values);
		case "long": return new LongMaker(values);
		case "int": return new IntMaker(values);
		case "double": return new DoubleMaker(values);
		case "float": return new FloatMaker(values);
		default:
			throw new RuntimeException("Invalid field type: " + type);
		}
	}
	
	// "[1-10]"
	private List<String> getParsedValues(String valueDef) {
		String bracketMatch = ".*(\\[[0-9]+[-][0-9]+\\]).*";
		if(!valueDef.matches(bracketMatch)) return List.of(valueDef);
		String brackets = valueDef.replaceAll(bracketMatch, "$1");
		String[] range = brackets.replaceAll(".*\\[([0-9]+)[-]([0-9]+)\\].*", "$1,$2").split(",");
		if(range.length != 2) return List.of(valueDef);
		int rangeStart = Integer.parseInt(range[0]);
		int rangeEnd = Integer.parseInt(range[1]) + 1;   // End is inclusive
		
		return IntStream.range(rangeStart, rangeEnd).mapToObj(v->valueDef.replace(brackets, Integer.toString(v))).toList();
	}
	
	record ColumnDef(String name, String type, String valueDef, Maker maker) {}
	
	class StringMaker extends Maker {
		StringMaker(List<String> values) {
			super(values.stream().map(v->(Object)v).toList());
		}
	}
	
	class LongMaker extends Maker {
		LongMaker(List<String> values) {
			super(values.stream().map(v->(Object)Long.valueOf(v)).toList());
		}
	}
	
	class IntMaker extends Maker {
		IntMaker(List<String> values) {
			super(values.stream().map(v->(Object)Integer.valueOf(v)).toList());
		}
	}
	
	class DoubleMaker extends Maker {
		DoubleMaker(List<String> values) {
			super(values.stream().map(v->(Object)Double.valueOf(v)).toList());
		}
	}
	
	class FloatMaker extends Maker {
		FloatMaker(List<String> values) {
			super(values.stream().map(v->(Object)Float.valueOf(v)).toList());
		}
	}

	abstract class Maker {
		final List<Object> values;
		Maker(List<Object> values) {
			this.values = values;
		}
		public Object next(long seed) {
			if(isFixed) return values.get((int)(seed % values.size()));
			return values.get(random.nextInt(0, values.size()));
		}
	}

}
