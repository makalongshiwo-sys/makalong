"""Encrypt the accepted signing key for its owner; only public recovery material is committed."""
from pathlib import Path
import os, subprocess, sys
key=Path(sys.argv[1]);dest=Path('releases');dest.mkdir(exist_ok=True)
material=os.urandom(48);temporary=key.with_suffix('.recovery-material');temporary.write_bytes(material)
try:
 subprocess.run(['openssl','enc','-aes-256-cbc','-K',material[:32].hex(),'-iv',material[32:].hex(),'-in',str(key),'-out',str(dest/'signing-key.encrypted')],check=True)
 subprocess.run(['openssl','pkeyutl','-encrypt','-pubin','-inkey','signing-recovery-public.pem','-in',str(temporary),'-out',str(dest/'signing-key.envelope'),'-pkeyopt','rsa_padding_mode:oaep','-pkeyopt','rsa_oaep_md:sha256'],check=True)
finally:temporary.unlink(missing_ok=True)
