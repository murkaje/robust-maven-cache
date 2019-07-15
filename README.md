Robust local cache
==================

Local maven cache may be corrupted by concurrent maven executions. While maven uses file locks during download, they might not be sufficient as seen in the wild on some cloud machines. 
By default maven does not validate artifacts in local cache, this is an easy low-hanging fruit to fix using a **maven core extension**

Installation
------------

Build the extension and copy the resulting jar to **MAVEN_INSTALL_DIR/lib/ext**

Maven will automatically pick up the extension and use it.  
Other installation options exist, most thorough instructions with examples seems to be in this changelog: https://maven.apache.org/docs/3.3.1/release-notes.html