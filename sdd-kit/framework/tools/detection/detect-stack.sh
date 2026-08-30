#!/bin/bash
# SDD SDD Kit - Stack Detection Script
# Detects the technology stack of a project
#
# Usage: detect-stack.sh [PROJECT_PATH] [--json] [--services-only] [--level]
#
# Options:
#   --json           Output as JSON
#   --services-only  Only detect  services (replaces detect-services.sh)
#   --level          Detect project level: hub | app | unknown

set -e

PROJECT_PATH="."
JSON_OUTPUT=false
SERVICES_ONLY=false
LEVEL_ONLY=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --json) JSON_OUTPUT=true; shift ;;
        --services-only) SERVICES_ONLY=true; shift ;;
        --level) LEVEL_ONLY=true; shift ;;
        -*)
            # Skip unknown flags
            shift ;;
        *)
            # First non-flag argument is PROJECT_PATH
            PROJECT_PATH="$1"
            shift ;;
    esac
done

if [ "$SERVICES_ONLY" = false ] && [ "$LEVEL_ONLY" = false ]; then
    echo "🔍 Detecting technology stack..."
    echo ""
fi

# Initialize detection results
LANGUAGE=""
FRAMEWORK=""
BUILD_TOOL=""
DATABASE=""
CACHE=""
MESSAGING=""
PLATFORM=""
FRONTEND_STACK=""
HAS_NORDIC=false
HAS_ANDES=false
HAS_ODIN=false

# Mobile Detection (runs BEFORE language detection)
# ============================================

detect_mobile() {
    # Android: AndroidManifest.xml (most reliable signal)
    if [ -f "$PROJECT_PATH/app/src/main/AndroidManifest.xml" ]; then
        LANGUAGE="kotlin"
        FRAMEWORK="jetpack-compose+everest+andes"
        BUILD_TOOL="gradle"
        PLATFORM="android"
        return 0
    fi

    # Android: your team-specific - mercadolibre.gradle.config plugin in root build file (.kts or Groovy)
    if grep -q "mercadolibre\.gradle\.config" "$PROJECT_PATH/build.gradle.kts" "$PROJECT_PATH/build.gradle" 2>/dev/null; then
        LANGUAGE="kotlin"
        FRAMEWORK="jetpack-compose+everest+andes"
        BUILD_TOOL="gradle"
        PLATFORM="android"
        return 0
    fi

    # Android: your team-specific - mercadolibre.gradle.config plugin in app-level build file (.kts or Groovy)
    if grep -q "mercadolibre\.gradle\.config" "$PROJECT_PATH/app/build.gradle.kts" "$PROJECT_PATH/app/build.gradle" 2>/dev/null; then
        LANGUAGE="kotlin"
        FRAMEWORK="jetpack-compose+everest+andes"
        BUILD_TOOL="gradle"
        PLATFORM="android"
        return 0
    fi

    # Android: your team-specific - Everest platform catalog in libs.versions.toml
    if [ -f "$PROJECT_PATH/gradle/libs.versions.toml" ] && grep -q "everestVersion" "$PROJECT_PATH/gradle/libs.versions.toml" 2>/dev/null; then
        LANGUAGE="kotlin"
        FRAMEWORK="jetpack-compose+everest+andes"
        BUILD_TOOL="gradle"
        PLATFORM="android"
        return 0
    fi

    # Android: standard com.android plugin in root build file (.kts or Groovy)
    if grep -q "com\.android" "$PROJECT_PATH/build.gradle.kts" "$PROJECT_PATH/build.gradle" 2>/dev/null; then
        LANGUAGE="kotlin"
        FRAMEWORK="jetpack-compose+everest+andes"
        BUILD_TOOL="gradle"
        PLATFORM="android"
        return 0
    fi

    # Android: your team-specific - mercadolibre.gradle.config.settings plugin in settings file (.kts or Groovy)
    if grep -q "mercadolibre\.gradle\.config" "$PROJECT_PATH/settings.gradle.kts" "$PROJECT_PATH/settings.gradle" 2>/dev/null; then
        LANGUAGE="kotlin"
        FRAMEWORK="jetpack-compose+everest+andes"
        BUILD_TOOL="gradle"
        PLATFORM="android"
        return 0
    fi

    # Android: settings.gradle.kts with include(":app") (Kotlin DSL - generic modern Android)
    # Guard: skip if definitive backend signals present (pom.xml, go.mod, package.json)
    if [ ! -f "$PROJECT_PATH/pom.xml" ] && [ ! -f "$PROJECT_PATH/go.mod" ] && [ ! -f "$PROJECT_PATH/package.json" ] && \
       [ -f "$PROJECT_PATH/settings.gradle.kts" ] && grep -q 'include(":app")' "$PROJECT_PATH/settings.gradle.kts" 2>/dev/null; then
        LANGUAGE="kotlin"
        FRAMEWORK="jetpack-compose+everest+andes"
        BUILD_TOOL="gradle"
        PLATFORM="android"
        return 0
    fi

    # Android: settings.gradle with include ':app' (Groovy DSL - generic Android)
    # Guard: skip if definitive backend signals present (pom.xml, go.mod, package.json)
    if [ ! -f "$PROJECT_PATH/pom.xml" ] && [ ! -f "$PROJECT_PATH/go.mod" ] && [ ! -f "$PROJECT_PATH/package.json" ] && \
       [ -f "$PROJECT_PATH/settings.gradle" ] && grep -q "include ':app'" "$PROJECT_PATH/settings.gradle" 2>/dev/null; then
        LANGUAGE="kotlin"
        FRAMEWORK="jetpack-compose+everest+andes"
        BUILD_TOOL="gradle"
        PLATFORM="android"
        return 0
    fi

    # iOS: your team-specific - EverestLibs pod in Podfile
    if [ -f "$PROJECT_PATH/Podfile" ] && grep -q "EverestLibs" "$PROJECT_PATH/Podfile" 2>/dev/null; then
        LANGUAGE="swift"
        FRAMEWORK="swiftui+everest+andes"
        BUILD_TOOL="xcodebuild"
        PLATFORM="ios"
        return 0
    fi

    # iOS: your team-specific - melisource spec repo or ML plugins in Podfile or Gemfile
    if grep -q "melisource" "$PROJECT_PATH/Podfile" "$PROJECT_PATH/Gemfile" 2>/dev/null; then
        LANGUAGE="swift"
        FRAMEWORK="swiftui+everest+andes"
        BUILD_TOOL="xcodebuild"
        PLATFORM="ios"
        return 0
    fi

    # iOS: your team-specific - cocoapods-controlquality plugin in Podfile or Gemfile
    if grep -q "cocoapods-controlquality" "$PROJECT_PATH/Podfile" "$PROJECT_PATH/Gemfile" 2>/dev/null; then
        LANGUAGE="swift"
        FRAMEWORK="swiftui+everest+andes"
        BUILD_TOOL="xcodebuild"
        PLATFORM="ios"
        return 0
    fi

    # iOS: .xcodeproj directory
    if ls -d "$PROJECT_PATH"/*.xcodeproj 2>/dev/null | head -1 | grep -q ".xcodeproj"; then
        LANGUAGE="swift"
        FRAMEWORK="swiftui+everest+andes"
        BUILD_TOOL="xcodebuild"
        PLATFORM="ios"
        return 0
    fi

    # iOS: .xcworkspace directory
    if ls -d "$PROJECT_PATH"/*.xcworkspace 2>/dev/null | head -1 | grep -q ".xcworkspace"; then
        LANGUAGE="swift"
        FRAMEWORK="swiftui+everest+andes"
        BUILD_TOOL="xcodebuild"
        PLATFORM="ios"
        return 0
    fi

    # iOS: Package.swift with iOS target
    if [ -f "$PROJECT_PATH/Package.swift" ] && grep -q "\.iOS\|\.tvOS\|\.watchOS" "$PROJECT_PATH/Package.swift" 2>/dev/null; then
        LANGUAGE="swift"
        FRAMEWORK="swiftui+everest+andes"
        BUILD_TOOL="xcodebuild"
        PLATFORM="ios"
        return 0
    fi

    return 1
}

# ============================================
# Language Detection
# ============================================

detect_language() {
    if [ -f "$PROJECT_PATH/pom.xml" ] || [ -f "$PROJECT_PATH/build.gradle" ]; then
        LANGUAGE="java"
        # Detect Java version
        if [ -f "$PROJECT_PATH/pom.xml" ]; then
            JAVA_VERSION=$(grep -oP '(?<=<java.version>)[^<]+' "$PROJECT_PATH/pom.xml" 2>/dev/null || echo "unknown")
        fi
    elif [ -f "$PROJECT_PATH/package.json" ]; then
        LANGUAGE="nodejs"
        # Check for TypeScript
        if [ -f "$PROJECT_PATH/tsconfig.json" ]; then
            LANGUAGE="typescript"
        fi
    elif [ -f "$PROJECT_PATH/requirements.txt" ] || [ -f "$PROJECT_PATH/pyproject.toml" ]; then
        LANGUAGE="python"
    elif [ -f "$PROJECT_PATH/go.mod" ]; then
        LANGUAGE="go"
    elif [ -f "$PROJECT_PATH/Cargo.toml" ]; then
        LANGUAGE="rust"
    else
        LANGUAGE="unknown"
    fi
}

# ============================================
# Build Tool Detection
# ============================================

detect_build_tool() {
    case "$LANGUAGE" in
        java)
            if [ -f "$PROJECT_PATH/pom.xml" ]; then
                BUILD_TOOL="maven"
            elif [ -f "$PROJECT_PATH/build.gradle" ]; then
                BUILD_TOOL="gradle"
            fi
            ;;
        nodejs|typescript)
            if [ -f "$PROJECT_PATH/yarn.lock" ]; then
                BUILD_TOOL="yarn"
            elif [ -f "$PROJECT_PATH/pnpm-lock.yaml" ]; then
                BUILD_TOOL="pnpm"
            else
                BUILD_TOOL="npm"
            fi
            ;;
        python)
            if [ -f "$PROJECT_PATH/poetry.lock" ]; then
                BUILD_TOOL="poetry"
            elif [ -f "$PROJECT_PATH/Pipfile" ]; then
                BUILD_TOOL="pipenv"
            else
                BUILD_TOOL="pip"
            fi
            ;;
        go)
            BUILD_TOOL="go-modules"
            ;;
    esac
}

# ============================================
# Framework Detection
# ============================================

detect_framework() {
    case "$LANGUAGE" in
        java)
            if [ -f "$PROJECT_PATH/pom.xml" ]; then
                if grep -q "spring-boot" "$PROJECT_PATH/pom.xml" 2>/dev/null; then
                    FRAMEWORK="spring-boot"
                    # Get Spring Boot version
                    FRAMEWORK_VERSION=$(grep -oP '(?<=<spring-boot.version>)[^<]+' "$PROJECT_PATH/pom.xml" 2>/dev/null || echo "")
                elif grep -q "quarkus" "$PROJECT_PATH/pom.xml" 2>/dev/null; then
                    FRAMEWORK="quarkus"
                elif grep -q "micronaut" "$PROJECT_PATH/pom.xml" 2>/dev/null; then
                    FRAMEWORK="micronaut"
                fi
            fi
            ;;
        nodejs|typescript)
            if [ -f "$PROJECT_PATH/package.json" ]; then
                if grep -q '"express"' "$PROJECT_PATH/package.json" 2>/dev/null; then
                    FRAMEWORK="express"
                elif grep -q '"fastify"' "$PROJECT_PATH/package.json" 2>/dev/null; then
                    FRAMEWORK="fastify"
                elif grep -q '"@nestjs/core"' "$PROJECT_PATH/package.json" 2>/dev/null; then
                    FRAMEWORK="nestjs"
                elif grep -q '"next"' "$PROJECT_PATH/package.json" 2>/dev/null; then
                    FRAMEWORK="nextjs"
                fi
            fi
            ;;
        python)
            if grep -q "fastapi\|FastAPI" "$PROJECT_PATH/requirements.txt" 2>/dev/null; then
                FRAMEWORK="fastapi"
            elif grep -q "django\|Django" "$PROJECT_PATH/requirements.txt" 2>/dev/null; then
                FRAMEWORK="django"
            elif grep -q "flask\|Flask" "$PROJECT_PATH/requirements.txt" 2>/dev/null; then
                FRAMEWORK="flask"
            fi
            ;;
        go)
            if grep -q "gin-gonic" "$PROJECT_PATH/go.mod" 2>/dev/null; then
                FRAMEWORK="gin"
            elif grep -q "echo" "$PROJECT_PATH/go.mod" 2>/dev/null; then
                FRAMEWORK="echo"
            elif grep -q "fiber" "$PROJECT_PATH/go.mod" 2>/dev/null; then
                FRAMEWORK="fiber"
            fi
            ;;
    esac
}

# ============================================
# Database Detection
# ============================================

detect_database() {
    # Check Java dependencies
    if [ -f "$PROJECT_PATH/pom.xml" ]; then
        if grep -q "postgresql" "$PROJECT_PATH/pom.xml" 2>/dev/null; then
            DATABASE="postgresql"
        elif grep -q "mysql" "$PROJECT_PATH/pom.xml" 2>/dev/null; then
            DATABASE="mysql"
        elif grep -q "mongodb" "$PROJECT_PATH/pom.xml" 2>/dev/null; then
            DATABASE="mongodb"
        fi
    fi

    # Check Node.js dependencies
    if [ -f "$PROJECT_PATH/package.json" ]; then
        if grep -q '"pg"\|"postgres"' "$PROJECT_PATH/package.json" 2>/dev/null; then
            DATABASE="postgresql"
        elif grep -q '"mysql"' "$PROJECT_PATH/package.json" 2>/dev/null; then
            DATABASE="mysql"
        elif grep -q '"mongodb"\|"mongoose"' "$PROJECT_PATH/package.json" 2>/dev/null; then
            DATABASE="mongodb"
        elif grep -q '"prisma"' "$PROJECT_PATH/package.json" 2>/dev/null; then
            DATABASE="prisma"
        fi
    fi

    # Check Python dependencies
    if [ -f "$PROJECT_PATH/requirements.txt" ]; then
        if grep -qi "psycopg\|postgresql" "$PROJECT_PATH/requirements.txt" 2>/dev/null; then
            DATABASE="postgresql"
        elif grep -qi "pymysql\|mysql" "$PROJECT_PATH/requirements.txt" 2>/dev/null; then
            DATABASE="mysql"
        elif grep -qi "pymongo\|mongodb" "$PROJECT_PATH/requirements.txt" 2>/dev/null; then
            DATABASE="mongodb"
        fi
    fi
}

# ============================================
# Frontend Stack Detection
# ============================================

detect_frontend_stack() {
    if [ -f "$PROJECT_PATH/package.json" ]; then
        # Check for Nordic
        if grep -q '"nordic"' "$PROJECT_PATH/package.json" 2>/dev/null; then
            HAS_NORDIC=true
        fi
        
        # Check for Andes (@andes/*)
        if grep -q '"@andes/' "$PROJECT_PATH/package.json" 2>/dev/null; then
            HAS_ANDES=true
        fi
        
        # Check for Odin (@odin/* or odin-*)
        if grep -qE '"@odin/|"odin-' "$PROJECT_PATH/package.json" 2>/dev/null; then
            HAS_ODIN=true
        fi
        
        # Determine frontend stack variant (in priority order)
        if [ "$HAS_NORDIC" = true ] && [ "$HAS_ODIN" = true ]; then
            FRONTEND_STACK="nordic-odin"
        elif [ "$HAS_NORDIC" = true ] && [ "$HAS_ANDES" = true ]; then
            FRONTEND_STACK="nordic-andes"
        elif [ "$HAS_NORDIC" = true ]; then
            FRONTEND_STACK="nordic"
        elif [ "$HAS_ANDES" = true ]; then
            FRONTEND_STACK="andes-only"
        fi
    fi
}

# ============================================
#  Services Detection
# ============================================

detect_services() {
    FURY_SERVICES=()

    # Search for  dependencies
    if grep -rq "fury-iam\|FuryIAM\|fury.iam" "$PROJECT_PATH/src" 2>/dev/null || \
       grep -q "fury-iam" "$PROJECT_PATH/pom.xml" 2>/dev/null; then
        FURY_SERVICES+=("iam")
    fi

    if grep -rq "fury-messaging\|FuryMessaging\|KafkaTemplate" "$PROJECT_PATH/src" 2>/dev/null || \
       grep -q "fury-messaging\|kafka" "$PROJECT_PATH/pom.xml" 2>/dev/null; then
        FURY_SERVICES+=("messaging")
        MESSAGING="kafka"
    fi

    if grep -rq "fury-cache\|FuryCache\|RedisTemplate" "$PROJECT_PATH/src" 2>/dev/null || \
       grep -q "fury-cache\|redis" "$PROJECT_PATH/pom.xml" 2>/dev/null; then
        FURY_SERVICES+=("cache")
        CACHE="redis"
    fi

    if grep -rq "KvsClient\|fury-kvs" "$PROJECT_PATH/src" 2>/dev/null; then
        FURY_SERVICES+=("kvs")
    fi

    if grep -rq "ObjectStorage\|fury-os" "$PROJECT_PATH/src" 2>/dev/null; then
        FURY_SERVICES+=("object-storage")
    fi
}

# ============================================
# Docker Detection
# ============================================

detect_docker() {
    DOCKER_CONFIG=""

    if [ -f "$PROJECT_PATH/Dockerfile" ]; then
        DOCKER_CONFIG="dockerfile"
        # Extract base image
        BASE_IMAGE=$(grep "^FROM" "$PROJECT_PATH/Dockerfile" | head -1 | awk '{print $2}')
    fi

    if [ -f "$PROJECT_PATH/Dockerfile.runtime" ]; then
        DOCKER_CONFIG="$DOCKER_CONFIG,dockerfile-runtime"
    fi

    if [ -f "$PROJECT_PATH/docker-compose.yml" ] || [ -f "$PROJECT_PATH/docker-compose.yaml" ]; then
        DOCKER_CONFIG="$DOCKER_CONFIG,docker-compose"
    fi
}

# ============================================
# CI/CD Detection
# ============================================

detect_cicd() {
    CICD_TOOL=""

    if [ -d "$PROJECT_PATH/.github/workflows" ]; then
        CICD_TOOL="github-actions"
    elif [ -f "$PROJECT_PATH/.gitlab-ci.yml" ]; then
        CICD_TOOL="gitlab-ci"
    elif [ -f "$PROJECT_PATH/Jenkinsfile" ]; then
        CICD_TOOL="jenkins"
    elif [ -f "$PROJECT_PATH/.fury.yml" ] || [ -f "$PROJECT_PATH/fury.yml" ]; then
        CICD_TOOL="fury-ci"
    fi
}

# ============================================
# Run Detection
# ============================================

# ============================================
# Level Detection (hub / app / unknown)
# ============================================

detect_level() {
    # Hub: meli/PROJECT.md exists AND contains ## Hub members heading
    if [ -f "$PROJECT_PATH/meli/PROJECT.md" ] && \
       grep -q "^## Hub members" "$PROJECT_PATH/meli/PROJECT.md" 2>/dev/null; then
        echo "hub"
        return
    fi

    # App: has typical app markers (.fury, src/, pom.xml, go.mod, package.json, etc.)
    if [ -f "$PROJECT_PATH/.fury" ] || \
       [ -d "$PROJECT_PATH/src" ] || \
       [ -f "$PROJECT_PATH/pom.xml" ] || \
       [ -f "$PROJECT_PATH/go.mod" ] || \
       [ -f "$PROJECT_PATH/package.json" ] || \
       [ -f "$PROJECT_PATH/requirements.txt" ] || \
       [ -f "$PROJECT_PATH/pyproject.toml" ] || \
       [ -f "$PROJECT_PATH/Cargo.toml" ] || \
       [ -f "$PROJECT_PATH/build.gradle" ] || \
       [ -f "$PROJECT_PATH/build.gradle.kts" ] || \
       [ -f "$PROJECT_PATH/Podfile" ] || \
       ls -d "$PROJECT_PATH"/*.xcodeproj 2>/dev/null | head -1 | grep -q ".xcodeproj" 2>/dev/null; then
        echo "app"
        return
    fi

    echo "unknown"
}

# Level-only mode: detect project level and exit
if [ "$LEVEL_ONLY" = true ]; then
    LEVEL=$(detect_level)
    if [ "$JSON_OUTPUT" = true ]; then
        echo "{\"level\":\"$LEVEL\"}"
    else
        echo "$LEVEL"
    fi
    exit 0
fi

# Services-only mode: only detect  services
if [ "$SERVICES_ONLY" = true ]; then
    detect_services

    if [ "$JSON_OUTPUT" = true ]; then
        if [ ${#FURY_SERVICES[@]} -eq 0 ]; then
            echo "{\"services\":[],\"count\":0}"
        else
            services_list=$(printf '"%s",' "${FURY_SERVICES[@]}" | sed 's/,$//')
            echo "{\"services\":[${services_list}],\"count\":${#FURY_SERVICES[@]}}"
        fi
    else
        echo "🔍  Services Detected"
        if [ ${#FURY_SERVICES[@]} -eq 0 ]; then
            echo "   No  services found"
        else
            for svc in "${FURY_SERVICES[@]}"; do
                echo "   ✓ $svc"
            done
        fi
        echo ""
        echo "Total: ${#FURY_SERVICES[@]} services"
    fi
    exit 0
fi

# Full stack detection: mobile check first (early return skips backend detection)
if ! detect_mobile; then
    detect_language
    detect_build_tool
    detect_framework
    detect_database
    detect_frontend_stack
    detect_services
fi
detect_docker
detect_cicd

# ============================================
# Output Results
# ============================================

if [ "$JSON_OUTPUT" = true ]; then
    # JSON output for programmatic use
    if [ ${#FURY_SERVICES[@]} -eq 0 ]; then
        services_json="[]"
    else
        services_json="[$(printf '"%s",' "${FURY_SERVICES[@]}" | sed 's/,$//' )]"
    fi

    cat << EOF
{
  "language": "$LANGUAGE",
  "buildTool": "$BUILD_TOOL",
  "framework": "$FRAMEWORK",
  "platform": "$PLATFORM",
  "database": "$DATABASE",
  "cache": "$CACHE",
  "messaging": "$MESSAGING",
  "furyServices": $services_json,
  "docker": "$DOCKER_CONFIG",
  "cicd": "$CICD_TOOL",
  "frontend": "$FRONTEND_STACK",
  "frontendDetails": {
    "nordic": $HAS_NORDIC,
    "andes": $HAS_ANDES,
    "odin": $HAS_ODIN
  }
}
EOF
else
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📊 Stack Detection Results"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "Language:    $LANGUAGE"
    echo "Platform:    ${PLATFORM:-backend}"
    echo "Build Tool:  $BUILD_TOOL"
    echo "Framework:   ${FRAMEWORK:-none detected}"
    echo "Frontend:    ${FRONTEND_STACK:-none detected}"
    if [ -n "$FRONTEND_STACK" ]; then
        echo "  Nordic:    $HAS_NORDIC"
        echo "  Andes:     $HAS_ANDES"
        echo "  Odin:      $HAS_ODIN"
    fi
    echo ""
    echo "Database:    ${DATABASE:-none detected}"
    echo "Cache:       ${CACHE:-none detected}"
    echo "Messaging:   ${MESSAGING:-none detected}"
    echo ""
    echo " Services: ${FURY_SERVICES[*]:-none detected}"
    echo ""
    echo "Docker:      ${DOCKER_CONFIG:-none}"
    echo "CI/CD:       ${CICD_TOOL:-none detected}"
    echo ""
fi
