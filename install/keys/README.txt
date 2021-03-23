
About GPG verification

1. Import public key

  Get the gpg public key from the key server.

  $ gpg --keyserver keyserver.ubuntu.com --recv-keys DFDE09F243437A38
  $ gpg --keyserver keys.gnupg.net --recv DFDE09F243437A38
  $ gpg --keyserver hkps.pool.sks-keyservers.net --recv DFDE09F243437A38

  Or download it from the repository.
  https://raw.githubusercontent.com/jpsonic/jpsonic/master/install/keys/webmaster%40tesshu.com.pem

  $ gpg --import C:\tmp\webmaster@tesshu.com.pem

2. Verify

  $ gpg --verify artifacts-checksums.sha.asc
