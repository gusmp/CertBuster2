package org.gusmp.CertBuster2.reporter;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

import org.gusmp.CertBuster2.beans.CertificateInfo;
import org.gusmp.CertBuster2.beans.IScanFileEntry;
import org.supercsv.cellprocessor.FmtDate;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;

@Getter
@Setter
public class CsvLine {

	public static final String[] header = new String[] { "Entry", "Object", "Type", "Subject", "Issuer", "serialNumber", "algorithmSignature",
			"notBefore", "notAfter", "crlStatus", "ocspStatus" };

	public static final CellProcessor[] processors = new CellProcessor[] { new NotNull(), new NotNull(), new NotNull(), new Optional(),
			new Optional(), new Optional(), new Optional(), new Optional(new FmtDate("dd/MM/yyyy hh:MM:ss")),
			new Optional(new FmtDate("dd/MM/yyyy hh:MM:ss")), new Optional(), new Optional() };

	private String entry;
	private String object = "";
	private String type = "";
	private String subject = "";
	private String issuer = "";
	private String serialNumber = "";
	private String algorithmSignature = "";
	private Date notBefore;
	private Date notAfter;
	private String crlStatus;	
	private String ocspStatus;

	private void setCommonFields(String object, IScanFileEntry entry) {
		this.entry = entry.getEntry();
		this.object = object;
		this.type = entry.getEntryType().name();
	}

	public CsvLine(CertificateInfo certificateInfo, String object, IScanFileEntry entry) {
		setCommonFields(object, entry);
		this.subject = certificateInfo.getCertificate().getSubjectX500Principal().getName();
		this.issuer = certificateInfo.getCertificate().getIssuerX500Principal().getName();
		this.serialNumber = certificateInfo.getCertificate().getSerialNumber().toString(16);
		this.algorithmSignature = certificateInfo.getCertificate().getSigAlgName();
		this.notBefore = certificateInfo.getCertificate().getNotBefore();
		this.notAfter = certificateInfo.getCertificate().getNotAfter();

		this.crlStatus = certificateInfo.getCrlStatus().getValue();
		this.ocspStatus = certificateInfo.getOCSPStatus().getValue();
	}

	public CsvLine(Exception exc, String object, IScanFileEntry entry) {
		setCommonFields(object, entry);
		this.subject = exc.getMessage();
	}

}
