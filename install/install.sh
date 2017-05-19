#!/bin/bash

# Check me out before you run me, I'm just gonna install Silk honest

# Borrowed from lein and emacs-live
HTTP_CLIENT=${HTTP_CLIENT:-"wget -O"}
if type -p curl >/dev/null 2>&1; then
  HTTP_CLIENT="curl $CURL_PROXY -f -k -L"
fi

# Create user home bin directory if it does not exist
if [ ! -d "$HOME/bin" ]; then
  mkdir -p $HOME/bin
fi

function download_tarball {
  echo ""
  echo $(tput setaf 2)"--> Downloading Silk..."$(tput sgr0)
  echo ""

  $HTTP_CLIENT https://github.com/silk-web-toolkit/silk/releases/download/0.11.0/silk.tgz -o $HOME/bin/silk.tgz
  tar xf $HOME/bin/silk.tgz -C $HOME/bin
  rm $HOME/bin/silk.tgz
}

download_tarball
