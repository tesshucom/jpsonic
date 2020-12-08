# About CVE verification

Jpsonic verifies CVE at build time.
Jpsonic is automatically compiled by Cron once a week. If CVE publishes information about threats, I'll notice it in up to a week or so.
Also apart from CVE validation at build time, Github may send high-impact risk alerts to developers.
In this case, correction or false positive suppression will be performed immediately.

# About CVE suppression

Software does not necessarily have to clear all the problems pointed out by CVE.
In reality, the software may not perform the processing that corresponds to the pointed out matter.
If only testing and compiling (rather than the execution phase), the threatening case may not hold.
And above all, it can be a false positive.

These management policies depend on the project.
If there is no need to upgrade the library or change the code, CVE threat reporting may be suppressed.
In the case of Jpsonic, it can be confirmed by tracking [the suppression setting file](https://github.com/tesshucom/jpsonic/blob/master/jpsonic-main/cve-suppressed.xml).

# Contact (Us)

If you have any questions, please contact us by email. 
However, it does not respond to application-level feature requests.
Keep in mind that sound network configuration and library updates are more important and essential for sound security.
