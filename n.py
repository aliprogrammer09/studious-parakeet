# Source - https://stackoverflow.com/a/3451150
# Posted by Rahul, modified by community. See post 'Timeline' for change history
# Retrieved 2026-10-22, License - CC BY-SA 4.0

import zipfile
with zipfile.ZipFile("untitled-app-oct-22-2026-3-07-13-pm.zip", 'r') as zip_ref:
    zip_ref.extractall("a")
