%define __jar_repack 0
%define __os_install_post %{nil}
Name: silk-web-toolkit
Version: %{version}
Release: %{release}
Group: System Environment/Daemons
Summary: Silk Web Toolkit
Vendor: github.com/silk-web-toolkit
License: Apache License v 2.0
Buildroot: %{_topdir}/BUILDROOT/%{name}-%{version}.%{_arch}

%description
Silk Web Toolkit.

%build


%install
cd %{_topdir}/../
mkdir $HOME/.silk
mkdir -p %{buildroot}/usr/bin
mkdir -p %{buildroot}/usr/lib/silk
cp -r target/*standalone* %{buildroot}/usr/lib/silk/silk.jar
cp -r build/etc/silk %{buildroot}/usr/bin

%files
%defattr (755,root,root,-)
/usr/bin/silk
/usr/lib/silk/silk.jar

%post


%preun
