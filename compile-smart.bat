@echo off
REM Build classpath from Maven repository
set M2_REPO=%USERPROFILE%\.m2\repository
set CLASSPATH=%M2_REPO%\jakarta\persistence\jakarta.persistence-api\3.1.0\jakarta.persistence-api-3.1.0.jar
set CLASSPATH=%CLASSPATH%;%M2_REPO%\org\projectlombok\lombok\1.18.30\lombok-1.18.30.jar
set CLASSPATH=%CLASSPATH%;%M2_REPO%\org\springframework\data\spring-data-jpa\3.1.3\spring-data-jpa-3.1.3.jar
set CLASSPATH=%CLASSPATH%;%M2_REPO%\org\springframework\spring-tx\6.1.1\spring-tx-6.1.1.jar
set CLASSPATH=%CLASSPATH%;%M2_REPO%\org\springframework\spring-orm\6.1.1\spring-orm-6.1.1.jar
set CLASSPATH=%CLASSPATH%;%M2_REPO%\org\springframework\spring-beans\6.1.1\spring-beans-6.1.1.jar
set CLASSPATH=%CLASSPATH%;%M2_REPO%\org\springframework\spring-core\6.1.1\spring-core-6.1.1.jar
set CLASSPATH=%CLASSPATH%;%M2_REPO%\org\springframework\spring-context\6.1.1\spring-context-6.1.1.jar
set CLASSPATH=%CLASSPATH%;%M2_REPO%\org\springframework\data\spring-data-commons\3.1.3\spring-data-commons-3.1.3.jar
set CLASSPATH=%CLASSPATH%;%M2_REPO%\org\springframework\security\spring-security-core\6.1.0\spring-security-core-6.1.0.jar
set CLASSPATH=%CLASSPATH%;%M2_REPO%\org\springframework\spring-web\6.1.1\spring-web-6.1.1.jar

echo Compiling Java files...
cd /d "%~dp0"

REM Compile entity classes
javac -cp "%CLASSPATH%" -d bin\src\main\java src\main\java\com\codingcomplex\demo\entities\Membership.java
javac -cp "%CLASSPATH%" -d bin\src\main\java src\main\java\com\codingcomplex\demo\entities\MembershipBenefit.java
javac -cp "%CLASSPATH%" -d bin\src\main\java src\main\java\com\codingcomplex\demo\entities\LandingPageContent.java

REM Compile repository interfaces
javac -cp "%CLASSPATH%" -d bin\src\main\java src\main\java\com\codingcomplex\demo\repositories\MembershipRepository.java
javac -cp "%CLASSPATH%" -d bin\src\main\java src\main\java\com\codingcomplex\demo\repositories\MembershipBenefitRepository.java
javac -cp "%CLASSPATH%" -d bin\src\main\java src\main\java\com\codingcomplex\demo\repositories\LandingPageContentRepository.java

REM Compile service class
javac -cp "%CLASSPATH%" -d bin\src\main\java src\main\java\com\codingcomplex\demo\services\LandingPageService.java

REM Compile controllers  
javac -cp "%CLASSPATH%" -d bin\src\main\java src\main\java\com\codingcomplex\demo\controllers\HomeController.java
javac -cp "%CLASSPATH%" -d bin\src\main\java src\main\java\com\codingcomplex\demo\controllers\LandingPageApiController.java

REM Compile config
javac -cp "%CLASSPATH%" -d bin\src\main\java src\main\java\com\codingcomplex\demo\config\SecurityConfig.java

echo Compilation complete!
pause
