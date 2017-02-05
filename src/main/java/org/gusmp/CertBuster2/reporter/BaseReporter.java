package org.gusmp.CertBuster2.reporter;

import org.gusmp.CertBuster2.BaseRoot;
import org.supercsv.prefs.CsvPreference;

public abstract class BaseReporter extends BaseRoot {

	protected CsvPreference getPreference() {
		return CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE;
	}

}
