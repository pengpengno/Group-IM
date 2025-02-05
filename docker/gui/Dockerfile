FROM ubuntu:20.04 AS builder

# 设置阿里云 apt 镜像源
RUN sed -i 's|http://archive.ubuntu.com|http://mirrors.aliyun.com|g' /etc/apt/sources.list && \
    sed -i 's|http://security.ubuntu.com|http://mirrors.aliyun.com|g' /etc/apt/sources.list && \
    apt-get update && \
    apt-get install -y \
    wget unzip curl git build-essential ninja-build \
    libglib2.0-dev libgtk-3-dev libpulse-dev libavcodec-dev libavformat-dev libavutil-dev libswscale-dev \
    libasound2-dev libavcodec-dev libavformat-dev libavutil-dev libgl-dev libgtk-3-dev libpango1.0-dev libxtst-dev \
    && rm -rf /var/lib/apt/lists/*

# 设置 GraalVM (Java 21) 环境变量
ENV JAVA_HOME=/usr/lib/jvm/graalvm \
    ANDROID_HOME=/opt/android-sdk \
    ANDROID_NDK_HOME=/opt/android-sdk/ndk/25.1.8937393 \
    PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

# 安装 GraalVM (Java 21)
RUN wget https://download.oracle.com/graalvm/21/latest/graalvm-jdk-21_linux-x64_bin.tar.gz -O graalvm.tar.gz \
    && tar -xzf graalvm.tar.gz -C /usr/lib/jvm \
    && mv /usr/lib/jvm/graalvm-jdk-21* /usr/lib/jvm/graalvm \
    && rm graalvm.tar.gz

# 安装 Android SDK 和 NDK
RUN mkdir -p $ANDROID_HOME/cmdline-tools && cd $ANDROID_HOME/cmdline-tools \
    && wget https://dl.google.com/android/repository/commandlinetools-linux-10406996_latest.zip -O cmdline-tools.zip \
    && unzip cmdline-tools.zip -d . \
    && mv cmdline-tools latest \
    && rm cmdline-tools.zip

# 接受 Android SDK 许可并安装 SDK 组件
RUN yes | sdkmanager --licenses && \
    sdkmanager "platform-tools" "platforms;android-31" "build-tools;31.0.0" "ndk;25.1.8937393" "cmake;3.22.1"

# 安装 GluonFX Maven 插件
RUN mkdir -p /root/.m2 && wget -O /root/.m2/settings.xml https://raw.githubusercontent.com/gluonhq/substrate/main/maven-settings.xml

# 设置工作目录
WORKDIR /app

# 复制项目文件
COPY . /app

# 构建 APK
RUN mvn clean install -DskipTests -s ./alisettings.xml
RUN mvn -Pandroid gluonfx:build gluonfx:package  -DskipTests -s ./alisettings.xml -am -f gui/pom.xml

# 生产环境阶段，只保留 APK
FROM ubuntu:20.04 AS runtime
WORKDIR /app

COPY --from=builder /app/target/*.apk /app/

CMD ["/bin/bash"]
