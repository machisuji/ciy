package wandledi.test;

import org.testng.annotations.BeforeSuite;

public class ExtSpellExperiment extends SpellExperiment {
	@BeforeSuite
	public void setTestPath() {
		SpellExperiment.DIR = "src/ext/wandledi/core/src/test/java/wandledi/test/";
	}
}
