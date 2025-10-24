#!/bin/bash

# Android Package Renamer Script with Auto Path Detection
# Usage: ./rename_package.sh <old_package> <new_package> [build_type]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to find Android project root
find_android_project_root() {
    local current_dir="$1"
    local dir="$current_dir"

    while [[ "$dir" != "/" ]]; do
        if [[ -f "$dir/build.gradle" || -f "$dir/build.gradle.kts" || -f "$dir/gradlew" ]]; then
            echo "$dir"
            return 0
        fi
        dir=$(dirname "$dir")
    done

    return 1
}

# Function to check if current directory is Android project
is_android_project() {
    local dir="$1"
    [[ -f "$dir/build.gradle" || -f "$dir/build.gradle.kts" || -f "$dir/gradlew" ]]
}

# Function to update file contents
update_file_contents() {
    local file="$1"
    local old_pkg="$2"
    local new_pkg="$3"

    if [[ -f "$file" ]]; then
        if grep -q "$old_pkg" "$file"; then
            sed -i.tmp "s/$old_pkg/$new_pkg/g" "$file"
            rm -f "$file.tmp"
            print_info "Updated: $file"
        fi
    fi
}

# Function to rename package directories
rename_package_dirs() {
    local project_dir="$1"
    local old_pkg="$2"
    local new_pkg="$3"

    local old_path="${old_pkg//.//}"
    local new_path="${new_pkg//.//}"

    print_info "Looking for package directories: $old_path"

    # Find all source directories that might contain the package structure
    find "$project_dir" -type d -path "*/src/*" \( -name "java" -o -name "kotlin" \) | while read src_dir; do
        local full_old_dir="$src_dir/$old_path"
        local full_new_dir="$src_dir/$new_path"

        if [[ -d "$full_old_dir" ]]; then
            print_info "Found package directory: $full_old_dir"
            mkdir -p "$(dirname "$full_new_dir")"
            mv "$full_old_dir" "$full_new_dir"
            print_success "Renamed: $full_old_dir -> $full_new_dir"

            # Remove empty parent directories
            find "$(dirname "$full_old_dir")" -type d -empty -delete 2>/dev/null || true
        fi
    done
}

# Function to update build.gradle files
update_gradle_files() {
    local project_dir="$1"
    local old_pkg="$2"
    local new_pkg="$3"

    find "$project_dir" -name "build.gradle" -o -name "build.gradle.kts" | while read gradle_file; do
        if [[ -f "$gradle_file" ]]; then
            local updated=false

            # Update applicationId
            if grep -q "applicationId.*[\"']$old_pkg[\"']" "$gradle_file"; then
                sed -i "s/applicationId[[:space:]]*[\"']$old_pkg[\"']/applicationId \"$new_pkg\"/g" "$gradle_file"
                updated=true
            fi

            # Update namespace (for newer Android projects)
            if grep -q "namespace.*[\"']$old_pkg[\"']" "$gradle_file"; then
                sed -i "s/namespace[[:space:]]*[\"']$old_pkg[\"']/namespace \"$new_pkg\"/g" "$gradle_file"
                updated=true
            fi

            if [[ "$updated" == true ]]; then
                print_info "Updated gradle file: $gradle_file"
            fi
        fi
    done
}

# Function to update AndroidManifest.xml
update_android_manifest() {
    local project_dir="$1"
    local old_pkg="$2"
    local new_pkg="$3"

    find "$project_dir" -name "AndroidManifest.xml" | while read manifest_file; do
        if [[ -f "$manifest_file" ]] && grep -q "$old_pkg" "$manifest_file"; then
            # Update package attribute and any package references
            sed -i "s/package=\"$old_pkg\"/package=\"$new_pkg\"/g" "$manifest_file"
            sed -i "s/android:name=\"$old_pkg/android:name=\"$new_pkg/g" "$manifest_file"
            print_info "Updated manifest: $manifest_file"
        fi
    done
}

# Function to build APK
build_apk() {
    local project_dir="$1"
    local build_type="${2:-release}"

    print_info "Building $build_type APK..."

    cd "$project_dir"

    if [[ -f "gradlew" ]]; then
        chmod +x gradlew
        ./gradlew clean "assemble${build_type^}"
    elif command -v gradle >/dev/null 2>&1; then
        gradle clean "assemble${build_type^}"
    else
        print_error "Neither gradlew nor gradle found. Cannot build APK."
        return 1
    fi

    # Find the generated APK
    local apk_path=$(find "$project_dir" -name "*.apk" -path "*/$build_type/*" | head -1)
    if [[ -n "$apk_path" ]]; then
        print_success "APK generated: $apk_path"
        echo "$apk_path"
    else
        print_warning "APK not found in expected location, searching everywhere..."
        local apk_path_any=$(find "$project_dir" -name "*.apk" | head -1)
        if [[ -n "$apk_path_any" ]]; then
            print_success "APK found: $apk_path_any"
            echo "$apk_path_any"
        else
            print_error "APK not found after build"
            return 1
        fi
    fi
}

# Function to detect current package name
detect_current_package() {
    local project_dir="$1"

    # Try to get from AndroidManifest.xml
    local manifest_file=$(find "$project_dir" -name "AndroidManifest.xml" | head -1)
    if [[ -f "$manifest_file" ]]; then
        local package_name=$(grep -oP 'package="\K[^"]+' "$manifest_file" | head -1)
        if [[ -n "$package_name" ]]; then
            echo "$package_name"
            return 0
        fi
    fi

    # Try to get from build.gradle
    local gradle_file=$(find "$project_dir" -name "build.gradle" -o -name "build.gradle.kts" | head -1)
    if [[ -f "$gradle_file" ]]; then
        local package_name=$(grep -oP 'applicationId\s+["'\'']\K[^"'\'']+' "$gradle_file" | head -1)
        if [[ -n "$package_name" ]]; then
            echo "$package_name"
            return 0
        fi
    fi

    return 1
}

# Main function
main() {
    # Auto-detect project path (current directory)
    PROJECT_PATH=$(pwd)

    print_info "Auto-detected project path: $PROJECT_PATH"

    # Validate this is an Android project
    if ! is_android_project "$PROJECT_PATH"; then
        print_error "Current directory is not an Android project!"
        print_error "No build.gradle, build.gradle.kts, or gradlew found."
        exit 1
    fi

    if [[ $# -lt 1 ]]; then
        echo "Usage: $0 <new_package> [build_type]"
        echo "       $0 <old_package> <new_package> [build_type]"
        echo ""
        echo "Examples:"
        echo "  $0 com.new.package"
        echo "  $0 com.new.package release"
        echo "  $0 com.old.package com.new.package debug"
        echo ""
        echo "If only one package is provided, the script will try to auto-detect current package."
        exit 1
    fi

    local old_package
    local new_package
    local build_type

    # Parse arguments
    if [[ $# -eq 1 ]]; then
        # Only new package provided, auto-detect old package
        new_package="$1"
        build_type="release"

        print_info "Auto-detecting current package name..."
        if old_package=$(detect_current_package "$PROJECT_PATH"); then
            print_info "Detected current package: $old_package"
        else
            print_error "Could not auto-detect current package name."
            print_error "Please provide both old and new package names."
            echo "Usage: $0 <old_package> <new_package> [build_type]"
            exit 1
        fi
    elif [[ $# -eq 2 ]]; then
        if [[ "$2" =~ ^(debug|release)$ ]]; then
            # <new_package> <build_type> format
            new_package="$1"
            build_type="$2"

            print_info "Auto-detecting current package name..."
            if old_package=$(detect_current_package "$PROJECT_PATH"); then
                print_info "Detected current package: $old_package"
            else
                print_error "Could not auto-detect current package name."
                print_error "Please provide both old and new package names."
                echo "Usage: $0 <old_package> <new_package> [build_type]"
                exit 1
            fi
        else
            # <old_package> <new_package> format
            old_package="$1"
            new_package="$2"
            build_type="release"
        fi
    else
        # <old_package> <new_package> <build_type> format
        old_package="$1"
        new_package="$2"
        build_type="$3"
    fi

    local old_path="${old_package//.//}"
    local new_path="${new_package//.//}"

    # Validate inputs
    if [[ -z "$old_package" || -z "$new_package" ]]; then
        print_error "Package names cannot be empty"
        exit 1
    fi

    if [[ "$old_package" == "$new_package" ]]; then
        print_error "Old and new package names are the same!"
        exit 1
    fi

    print_info "Starting package rename..."
    print_info "Project: $PROJECT_PATH"
    print_info "Old package: $old_package"
    print_info "New package: $new_package"
    print_info "Build type: $build_type"

    # Confirm action
    read -p "Continue with package rename? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_info "Operation cancelled by user"
        exit 0
    fi

    # Backup the project
    local backup_dir="${PROJECT_PATH}_backup_$(date +%Y%m%d_%H%M%S)"
    print_info "Creating backup: $backup_dir"
    cp -r "$PROJECT_PATH" "$backup_dir"

    # Update gradle files first
    print_info "Updating gradle files..."
    update_gradle_files "$PROJECT_PATH" "$old_package" "$new_package"

    # Update AndroidManifest.xml
    print_info "Updating AndroidManifest.xml files..."
    update_android_manifest "$PROJECT_PATH" "$old_package" "$new_package"

    # Find and update all source files
    print_info "Updating source files..."
    find "$PROJECT_PATH" \( -name "*.java" -o -name "*.kt" -o -name "*.xml" -o -name "*.gradle" -o -name "*.kts" -o -name "*.pro" -o -name "*.json" \) -type f | while read source_file; do
        update_file_contents "$source_file" "$old_package" "$new_package"
    done

    # Rename package directories
    print_info "Renaming package directories..."
    rename_package_dirs "$PROJECT_PATH" "$old_package" "$new_package"

    # Build APK
    if apk_path=$(build_apk "$PROJECT_PATH" "$build_type"); then
        print_success "Package rename and APK generation completed successfully!"
        print_success "Backup created: $backup_dir"
        print_success "APK location: $apk_path"
    else
        print_error "Build failed. Restoring from backup..."
        rm -rf "$PROJECT_PATH"
        mv "$backup_dir" "$PROJECT_PATH"
        exit 1
    fi
}

# Run main function with all arguments
main "$@"