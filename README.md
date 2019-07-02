[![](https://jitpack.io/v/showang/Respect-OkHttp.svg)](https://jitpack.io/#showang/Respect-OkHttp)
# Respect-OkHttp
A OkHttp3 request executor based on [Respect framework](https://github.com/showang/Respect).

# How to
To get a Git project into your build:

## Step 1. Add the JitPack repository to your build file

gradle
maven
sbt
leiningen
Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
## Step 2. Add the dependency

	dependencies {
	        implementation 'com.github.showang:Respect-OkHttp:{lastversion}'
	}
