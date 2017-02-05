package org.gusmp.CertBuster2.service;

import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.gusmp.CertBuster2.beans.CertificateInfo;
import org.gusmp.CertBuster2.service.connection.BaseConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MailService extends BaseConnection {

	@Value("${certbuster2.smtp.host}")
	private String smtpHost;

	@Value("${certbuster2.smtp.starttls.enabled}")
	private boolean enableStartTls;

	@Value("${certbuster2.smtp.port}")
	private int smtpPort;

	@Value("${certbuster2.smtp.authentication}")
	private boolean authentication;

	@Value("${certbuster2.smtp.user}")
	private String smtpUser;

	@Value("${certbuster2.smtp.password}")
	private String smtpPassword;

	@Value("${certbuster2.mail.from}")
	private String mailFrom;

	@Value("#{'${certbuster2.mail.to}'.split(',')}")
	private String[] mailTo;

	/*
	 * private void setProxyConfiguration() {
	 * 
	 * System.setProperty("http.proxyHost", proxyHost);
	 * System.setProperty("http.proxyPort", String.valueOf(proxyPort));
	 * 
	 * System.setProperty("https.proxyHost", proxyHost);
	 * System.setProperty("https.proxyPort", String.valueOf(proxyPort));
	 * 
	 * logger.debug("Setted http(s).proxyHost / http(s).proxyPort setting");
	 * 
	 * if ((proxyUser.isEmpty() == false) && (proxyPassword.isEmpty() == false))
	 * {
	 * 
	 * logger.debug("Setted authentication settings");
	 * Authenticator.setDefault(new Authenticator() {
	 * 
	 * @Override protected PasswordAuthentication getPasswordAuthentication() {
	 * return new PasswordAuthentication(proxyUser,
	 * proxyPassword.toCharArray()); } }); } }
	 */

	public void sendMail_SSL(String subject, String body, List<String> attachmentList) {

		Properties props = new Properties();
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.socketFactory.port", "465");
		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", "465");
		props.put("mail.smtp.debug", "true");

		props.put("mail.smtp.starttls.enable", "true");

		Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
			protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
				return new javax.mail.PasswordAuthentication("seneka25@gmail.com", "tormentaA1");
			}
		});

		session.setDebug(true);

		try {

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress("seneka2555@outlook.com"));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("seneka25@gmail.com"));
			message.setSubject("Testing Subject");
			message.setText("Dear Mail Crawler," + "\n\n No spam to my email, please!");

			Transport.send(message);

			System.out.println("Done");
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}

		/*
		 * Properties props = new Properties(); props.put("mail.smtp.auth",
		 * "true"); props.put("mail.smtp.host", smtpHost);
		 * props.put("mail.smtp.port", smtpSslPort);
		 * props.put("mail.smtp.socketFactory.port", smtpSslPort);
		 * props.put("mail.smtp.socketFactory.class",
		 * "javax.net.ssl.SSLSocketFactory"); props.put("mail.smtp.debug",
		 * "true");
		 * 
		 * Session session = Session.getDefaultInstance(props, new
		 * javax.mail.Authenticator() { protected
		 * javax.mail.PasswordAuthentication getPasswordAuthentication() {
		 * return new javax.mail.PasswordAuthentication(smtpUser,smtpPassword);
		 * } }); session.setDebug(true);
		 * 
		 * try {
		 * 
		 * MimeMessage message = new MimeMessage(session); message.setFrom(new
		 * InternetAddress(mailFrom));
		 * //message.setRecipients(Message.RecipientType
		 * .TO,InternetAddress.parse("seneka25@gmail.com")); for (String to :
		 * mailTo) { message.addRecipient(Message.RecipientType.TO, new
		 * InternetAddress(to)); }
		 * 
		 * message.setSubject("SSL: " + subject); message.setText(body, "UTF-8",
		 * "html");
		 * 
		 * Transport.send(message);
		 * 
		 * System.out.println("Done");
		 * 
		 * } catch (MessagingException e) { throw new RuntimeException(e); }
		 */
	}

	public void sendMail(String subject, String body, List<String> attachmentList) {

		logger.info("Sending email: " + subject);

		Properties props = new Properties();
		props.put("mail.smtp.auth", authentication);
		props.put("mail.smtp.host", smtpHost);
		props.put("mail.smtp.port", smtpPort);
		props.put("mail.smtp.socketFactory.port", smtpPort);
		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.debug", "true");

		props.put("mail.smtp.starttls.enable", enableStartTls);
		props.put("mail.smtp.ssl.trust", "*");

		Session session = null;

		if (authentication) {
			session = Session.getInstance(props, new javax.mail.Authenticator() {
				protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
					return new javax.mail.PasswordAuthentication(smtpUser, smtpPassword);
				}
			});
		} else {
			session = Session.getInstance(props);
		}
		session.setDebug(true);

		try {

			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(mailFrom));
			for (String to : mailTo) {
				message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
			}
			message.setSubject("TLS: " + subject);
			message.setText(body);

			Multipart multipart = new MimeMultipart();
			for (String attachment : attachmentList) {

				BodyPart messageBodyPart = new MimeBodyPart();
				DataSource source = new FileDataSource(attachment);
				messageBodyPart.setDataHandler(new DataHandler(source));
				messageBodyPart.setFileName(attachment);

				multipart.addBodyPart(messageBodyPart);
			}
			message.setContent(multipart);

			Transport.send(message);

			logger.info("Email '" + subject + "' sent");

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * public void sendMail(String body, List<String> attachmentList) {
	 * 
	 * logger.info("Sending email");
	 * 
	 * try {
	 * 
	 * Properties props = new Properties();
	 * 
	 * props.setProperty("mail.smtp.host", smtpHost);
	 * props.setProperty("mail.smtp.port", String.valueOf(smtpPort));
	 * 
	 * if (enableStartTls == true) {
	 * props.setProperty("mail.smtp.starttls.enable", "true");
	 * props.setProperty("mail.smtp.ssl.trust", "*"); } else {
	 * props.setProperty("mail.smtp.starttls.enable", "false"); }
	 * 
	 * 
	 * //https://support.google.com/mail/answer/78754
	 * //props.setProperty("https.protocols", "TLSv1.1,TLSv1.2,SSLv3"); //
	 * indiferente props.setProperty("mail.smtp.auth",
	 * String.valueOf(smtpAuth));
	 * 
	 * props.put("mail.smtp.socketFactory.port", "587");
	 * props.put("mail.smtp.socketFactory.class",
	 * "javax.net.ssl.SSLSocketFactory");
	 * 
	 * if (useProxy == true) { logger.info("Enable proxy for sending emails");
	 * setProxyConfiguration(); }
	 * 
	 * props.setProperty("mail.smtp.debug", "true"); //Session session =
	 * Session.getDefaultInstance(props);
	 * 
	 * Session session = Session.getInstance(props, new
	 * javax.mail.Authenticator() { protected javax.mail.PasswordAuthentication
	 * getPasswordAuthentication() { return new
	 * javax.mail.PasswordAuthentication(smtpUser, smtpPassword); } });
	 * 
	 * 
	 * session.setDebug(true); //System.setProperty("javax.net.debug", "ssl");
	 * 
	 * MimeMessage message = new MimeMessage(session);
	 * 
	 * message.setFrom(new InternetAddress(mailFrom)); for (String to : mailTo)
	 * { message.addRecipient(Message.RecipientType.TO, new
	 * InternetAddress(to)); }
	 * 
	 * message.setSubject("SSL certificates mail status2");
	 * message.setText("body", "UTF-8", "html");
	 * 
	 * Transport.send(message);
	 * 
	 * } catch (Exception exc) { logger.error("Error send mail to " + mailTo[0]
	 * + ". " + exc.toString()); } }
	 */

	@Override
	public CertificateInfo getCertificate(String host, Integer port) throws Exception {
		return null;
	}

}
