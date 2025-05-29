FROM ubuntu:22.04 AS builder
# windows  环境下使用 镜像惊醒编译
# 设置时区为 UTC+8 (Asia/Shanghai)
RUN apt-get update && apt-get install -y tzdata  bash coreutils && \
    ln -fs /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    dpkg-reconfigure --frontend noninteractive tzdata

# 设置阿里云 apt 镜像源
RUN sed -i 's|http://archive.ubuntu.com|http://mirrors.aliyun.com|g' /etc/apt/sources.list && \
    sed -i 's|http://security.ubuntu.com|http://mirrors.aliyun.com|g' /etc/apt/sources.list && \
    apt-get update && \
    apt-get install -y \
    wget unzip curl git build-essential ninja-build \
    libglib2.0-dev libgtk-3-dev libpulse-dev libavcodec-dev libavformat-dev libavutil-dev libswscale-dev \
    libasound2-dev libavcodec-dev libavformat-dev libavutil-dev libgl-dev libgtk-3-dev libpango1.0-dev libxtst-dev \
#    openjdk-21-jdk  \
    maven \
    && rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /app

COPY graalvm.tar.gz /app/graalvm.tar.gz
#RUN tar -xzf graalvm.tar.gz -C /usr/lib/jvm \
#    && mv /usr/lib/jvm/graalvm-jdk-21* /usr/lib/jvm/graalvm \
#    && rm graalvm.tar.gz \

# 设置 GraalVM (Java 21) 环境变量
ENV JAVA_HOME=/usr/lib/jvm/graalvm \
    GRAALVM_HOME=/usr/lib/jvm/graalvm\
    ANDROID_HOME=/opt/android-sdk \
    ANDROID_NDK_HOME=/opt/android-sdk/ndk/25.1.8937393 \
    PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

RUN tar -xzf graalvm.tar.gz -C /usr/lib/jvm \
    && mv /usr/lib/jvm/graalvm-* /usr/lib/jvm/graalvm \
    && rm graalvm.tar.gz
#

# 设置环境变量
ENV GRAALVM_HOME=/usr/lib/jvm/graalvm
ENV JAVA_HOME=$GRAALVM_HOME
ENV PATH="$GRAALVM_HOME/bin:$PATH"



# 安装 Android SDK 和 NDK
RUN mkdir -p $ANDROID_HOME/cmdline-tools && cd $ANDROID_HOME/cmdline-tools \
    && wget https://dl.google.com/android/repository/commandlinetools-linux-10406996_latest.zip -O cmdline-tools.zip \
    && unzip cmdline-tools.zip -d . \
    && mv cmdline-tools latest \
    && rm cmdline-tools.zip

# 接受 Android SDK 许可并安装 SDK 组件

#RUN yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_HOME --licenses && \
#    $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_HOME \
#        "platform-tools" \
#        "platforms;android-33" \
#        "build-tools;33.0.2" \
#        "ndk;25.1.8937393"

# 安装 GluonFX Maven 插件

# 设置工作目录
WORKDIR /app

# 复制项目文件
#COPY ../ /app
# 构建 APK
RUN export ANDROID_SDK=$ANDROID_HOME

ENTRYPOINT ["/bin/bash", "-c", "while true; do sleep 10000; done"]
