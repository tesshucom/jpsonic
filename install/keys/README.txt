About GPG verification

To ensure the integrity and authenticity of the distributed files, 
all binaries are signed with the developer's GPG key.

1. Import the Public Key

You can import the developer's public key from a keyserver or 
directly from the repository.

[From keyserver]
$ gpg --keyserver keyserver.ubuntu.com --recv-keys DFDE09F243437A38

(Or From Repository)
$ curl -sS https://raw.githubusercontent.com/jpsonic/jpsonic/master/install/keys/webmaster%40tesshu.com.pem | gpg --import

2. Verify the Signature and Integrity

Run the following commands in the directory where you downloaded 
the .war and .asc files. (Example for JDK 21)

$ gpg --verify jpsonic-java21.sha.asc
$ sha256sum -c jpsonic-java21.sha.asc

If the output shows "Good signature" and "OK", the file is 
authentic and has not been tampered with.

------------------------------------------------------------
Developer Fingerprint:
527B B1E5 00E2 8F65 1A19 7A48 DFDE 09F2 4343 7A38
------------------------------------------------------------
