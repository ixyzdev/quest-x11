FROM eclipse-temurin:17.0.21.1_2-jdk-noble AS quest-x11-sdk

ARG COMMAND_LINE_TOOLS_ARCHIVE=commandlinetools-linux-16111833_latest.zip
ENV ANDROID_HOME=/opt/android-sdk

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip \
    && rm -rf /var/lib/apt/lists/*

RUN curl -fsSL -o /tmp/command-line-tools.zip "https://dl.google.com/android/repository/${COMMAND_LINE_TOOLS_ARCHIVE}" \
    && mkdir -p "${ANDROID_HOME}/cmdline-tools" \
    && unzip -q /tmp/command-line-tools.zip -d "${ANDROID_HOME}/cmdline-tools" \
    && mv "${ANDROID_HOME}/cmdline-tools/cmdline-tools" "${ANDROID_HOME}/cmdline-tools/latest" \
    && rm /tmp/command-line-tools.zip

RUN yes | "${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null \
    && "${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" \
        "platforms;android-34" \
        "build-tools;36.0.0" \
        "platform-tools" \
        "cmake;3.22.1" \
        "ndk;29.0.14206865"

FROM eclipse-temurin:17.0.21.1_2-jdk-noble AS quest-x11-builder

ENV ANDROID_HOME=/opt/android-sdk

RUN apt-get update \
    && apt-get install -y --no-install-recommends git patch python3 bison gcc libc6-dev \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --create-home builder \
    && mkdir -p /home/builder/workspace /home/builder/.gradle /home/builder/apk \
    && chown -R builder:builder /home/builder

COPY --from=quest-x11-sdk --chown=builder:builder /opt/android-sdk /opt/android-sdk
COPY --chmod=755 docker/build-apk.sh /usr/local/bin/build-apk.sh

USER builder
WORKDIR /home/builder/workspace

ENTRYPOINT ["/usr/local/bin/build-apk.sh"]
