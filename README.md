# __CertBuster2__

### What Certbuster2 is?

Certbuster2 is a command line tool whose main purpose is discover digital certificates, not only installed in web servers, but also spread in the file system.
___

### What Certbuster2 can do?
Here is a list of what this tool can do:

* Find installed certificates installed in webservers
* Find certificated in the file system
* Resume previous scan
* Validate certificate using OCSP or CRL
* Reporting:
  * Report of what was discovered (full report)
  * Report of the upcoming expired certificates
* Send reports by email
---

### Requirements

To run *Certbuster2* you need:  

* Java environment 1.7 or upper
* (optional) Third party tools such as OpenSSL, gnuTLS or LibreSSL

>  Certbuster2 can use Java classes to do the SSL connection. If your java installation is old, this might prevent from discovering certificates. For example, JDK 1.6 (no supported) does not support TLS 1.1 nor TLS 1.2. The safest option is to use an updated Java version.  

___

### For impatients: quickstart

Download from the *bin* folder the jar file *CertBuster2-X.Y.Z.jar*. Create a file *scanfile.txt* and add websites/ip address or files/folders. Then execute:

>  java -jar CertBuster2-X.Y.Z.jar

As a result, *Certbuster2* will generate some csv files which contains what has been discovered. These files can be opened with Calc, Excel or any other spreadsheet.
___

### How to define what to scan?

All the places where *Certbuster2* should look for certificates must be included in the file *scanfile.txt*. Currently, Certbuster2 admits the following types of entries:


Type of entry  | Description |  Example
-------------- | -------------|--------
File  | just a file  | /myPath/myCert.cert
Folder | Scan all files/folders recursively | /myPath
domain or ip | domain or ip | www.google.com or 192.168.4.56
network ip | network ip | 192.168.0.0/24

By default, network entries **scan only port 443**. You might be interested in other port or even a range of ports. To fulfill such requirement you can add to your network entries a list of ports or ranges. See the following examples:

Entry | Port scanned
------|--------
ip / network ip:444| 444 only
ip / network ip:443,444| 443 and 444
ip / network ip:443-450| from 443 to 450 both included
ip / network ip:443,444,500-505| 443,444, from 500 to 505 both included

You can add as many ports, rages in any order.
___

### Tunning CertBuster2

There are many reasons for why you would like to tune Certbuster2, for example to speed up your scans. Create a file called *application.properties* in the same folder of *CertBuster2-X.Y.Z.jar*. The following table explains all the properties can be adjusted when scanning.

Entry | Description | Default value
------|-------------|--------------
_General_ ||
certbuster2.discovery.scanFile | File with places to scan  | scanfile.txt
certbuster2.resume.baseResumeFile | File to save scan stated. Useful to resume a previous session  | resume.txt
_File system_ ||
certbuster2.fileSystem.allowedExtensions 	| Comma separated list of extensions. Only file ending with theses  will be scanned  |*
certbuster2.fileSystem.maxSize 		| Maximum file size | 2400
_Reporting_ ||
certbuster2.reporting.fullCsv.enabled	| Generate report of found certificates (full report) | true
certbuster2.reporting.fullCsv.baseReportFile	| File of the full report  | report.csv
certbuster2.reporting.fullCsv.sendReportByEmail	| Send full report by email	| false
certbuster2.reporting.futureCaducityCsv.enabled	| Generate report of upcoming expired certificates | false
certbuster2.reporting.futureCaducityCsv.baseReportFile | File of the upcoming expired certificates report | report.csv
certbuster2.reporting.futureCaducityCsv.periods	| List of days to check. It's to say there will be 3 report, one for the expired certificates in 30 days, 60 days and 90 days. | 30,60,90
certbuster2.reporting.futureCaducityCsv.sendReportByEmail	| Send  upcoming expired certificates report by email | false
_Connection_ (See _More about Connection services_)  ||
certbuster2.connection.connectionChain | Certbuster2 is capable of obtain certificates using several methods. Valid values are: NATIVE, COMMONS, BC, GNUTLS, OPENSSL, LIBRESSL | NATIVE, COMMONS
certbuster2.connection.gnuTLSCastleConnectionService.gnutlsClientExecutable	| Path of gnutls-cli executable | ./external/GnuTLS/gnutls-cli.exe
certbuster2.connection.openSSLCastleConnectionService.openSSLExecutable | Path of openssl executable | ./external/openssl/openssl.exe
certbuster2.connection.openSSLCastleConnectionService.libreSSLExecutable 	| Path of libressl executable | ./external/libreSSL/openssl.exe
certbuster2.connection.timeout	| Maximum time in miliseconds to get a certificate | 5000
certbuster2.connection.reTryNumber | Number of attempt |	1
_Network parameters_ ||
certbuster2.proxy.useProxy	| Use proxy | false
certbuster2.proxy.host | Proxy host |
certbuster2.proxy.port | Proxy port |
certbuster2.proxy.user | Proxy user |
certbuster2.proxy.password | Proxy password |
_Certificate validation_ ||
certbuster2.certificate.validate | Validate found certificated | false
certbuster2.certificate.validateChain	| Validation methods. Valid values are OCSP and CRL. The order is important because the methods will be applied in this order | OCSP, CRL
certbuster2.certificate.enableCrlCache	|Cache crl files| true
_SMTP settings_ (See _Further smtp configuration_) |
certbuster2.smtp.port | Smtp port | 587
certbuster2.mail.to | Destination address |
certbuster2.smtp.authentication  | Enable authentication | true
certbuster2.smtp.starttls.enabled	| Enable tls | true
certbuster2.smtp.host | SMTP host |
certbuster2.smtp.user | SMTP user |
certbuster2.smtp.password | SMTP password |
certbuster2.mail.from | Sender address | certbuster2@no-reply.com

### More about Connection services

*Certbuster2* can get remote certificate using several connection services:

* Native java classes (NATIVE)
* Apache Components HTTPClient (COMMONS)
* Bouncy Castle (BC)
* Gnu TLS (GNUTLS)
* OpenSSL (OPENSSL)
* LibreSSL (LIBRESSL)

Choose one or another can

Connection Service | Ranking fastest connection service (if certificate is available) | Maximum time to get the certificate | Require external components | Support proxy | TLS implementation
-------------------|------------------------------------------------------------------|-------------------------------------|-----------------------------|---------------|-------------------
NATIVE | 3| Defined in timeout property | No | Yes | Java Secure Socket Extension (JSSE)
COMMONS | 6| Defined in timeout property | No | Yes  | Java Secure Socket Extension (JSSE)
BC | 5| Defined in timeout property | No | Probably not  | Java Secure Socket Extension (JSSE)
Gnu TLS | 2| Defined in timeout property | [Yes](https://www.gnutls.org/download.html) | Yes as long as it uses http_proxy/ HTTP_PROXY environment variable  | GnuTLS
OpenSSL | 4| Defined in timeout property | [Yes](https://www.openssl.org/) | No | OpenSSL
LibreSSL | 1| Defined in timeout property | [Yes](https://www.libressl.org/) | Yes | LibreSSL



### Further SMTP configuration

This section explains specific settings for different popular mail servers.

## Outlook

NOTE: Enable SMPT usage from your account settings

Entry | Suggested value  
------|-------------
certbuster2.smtp.port | 587
certbuster2.mail.to | your address
certbuster2.smtp.authentication | true
certbuster2.smtp.starttls.enabled	| true
certbuster2.smtp.host | smtp-mail.outlook.com
certbuster2.smtp.user | your user
certbuster2.smtp.password | your password
certbuster2.mail.from  | certbuster2@no-reply.com


## Gmail

NOTE: You have to activate less secure apps from [here (https://www.google.com/settings/security/lesssecureapps)](https://www.google.com/settings/security/lesssecureapps)


Entry | Suggested value  
------|-------------
certbuster2.smtp.port | 587
certbuster2.mail.to | your address
certbuster2.smtp.authentication | true
certbuster2.smtp.starttls.enabled	| true
certbuster2.smtp.host | smtp.gmail.com
certbuster2.smtp.user | your user
certbuster2.smtp.password | your password
certbuster2.mail.from  | certbuster2@no-reply.com
