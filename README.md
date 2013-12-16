iiCaptain
=========



Attention and changes to the requirements below:

Adobe just went live with build.phonegap.com on September 24th and they changed a couple of things with the 1.0 release.
Initially the demo did a file upload which is not available for opensource apps anylonger. Therefore the src files for phonegap
need to be available via git.
Therefore on your SCM/Jenkins VM you also need git installed (>=1.7.9). The maven pom contains a plugin which will push the src files of the client
to git and then request a pull and build from build.phonegap.com via REST.
In the docs folder you find one .get-credentials file. Please copy it to the root folder of the user executing jenkins on the SCM/Jenkins VM. Then open the file and replace "yourmail%40address.com" and "YourPassword" with your github credentials.
In the build.xml there is my password for phonegap missing. Please send a mail to digitalemil@googlemail.com if you want to have it. Otherwise please create an account in phonegap and you these credentials (you also need to set the property appId in build.xml to reflect your app). 



If you want to build different client apps you need to register with build.phonegap.com and modify the token and AppIDs in build.xml


Have fun!

 Emil





