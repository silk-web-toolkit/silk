BUILD INSTRUCTIONS

This file details builds across various deployment platforms.  As an efficiency gain we use alien to transform an rp minto a deb.

=====
Linux
=====

RPM
---

(assuming you are in the build directory)
cd rpm
rpmbuild --define 'version 0.5.0' --define 'release develop' --define "_topdir $PWD/rpmbuild" -bb silk.spec --target noarch


DEB
---

(assuming you are in the build directory)
cd ../rpmbuild/RPMS/noarch
sudo alien silkxyz.rpm
