#!/bin/bash

# SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
#
# SPDX-License-Identifier: AGPL-3.0-only

# Script to bump version in both PKGBUILD and YAML files
# Usage: ./bump-version.sh <new_version>

set -euo pipefail

SCRIPT_NAME=$(basename "$0")
usage() {
    echo "Usage: $SCRIPT_NAME <new_version>"
    echo ""
    echo "Examples:"
    echo "  $SCRIPT_NAME 1.2.3"
    echo "  $SCRIPT_NAME 2.0.0-beta1"
    echo ""
    echo "This script will update:"
    echo "  - pkgver in package/PKGBUILD file"
    echo "  - version in carbonio-ws-collaboration-openapi/src/main/resources/api.yaml"
    echo "  - x-supported-versions in YAML files (adds new version at the beginning)"
    exit 1
}
# Check if version argument is provided
if [ $# -ne 1 ]; then
    echo "Error: Version argument is required"
    echo ""
    usage
fi
NEW_VERSION="$1"
# Validate version format (basic semver-like pattern)
if ! [[ "$NEW_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9]+)?$ ]]; then
    echo "Warning: Version '$NEW_VERSION' doesn't follow semantic versioning format"
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Aborted."
        exit 1
    fi
fi

update_pkgbuild() {
    local file="package/PKGBUILD"

    if [ ! -f "$file" ]; then
        echo "Warning: $file not found, skipping..."
        return 0
    fi

    # Check if pkgver exists in the file
    if ! grep -q "^pkgver=" "$file"; then
        echo "Warning: 'pkgver=' not found in $file, skipping..."
        return 0
    fi

    # Get current version
    local current_version
    current_version=$(grep "^pkgver=" "$file" | cut -d'=' -f2)
    echo "PKGBUILD: $current_version -> \"$NEW_VERSION\""

    # Update pkgver
    sed -i "" "s/^pkgver=.*/pkgver=\"$NEW_VERSION\"/" "$file"

    # Reset pkgrel to 1 if it exists
    if grep -q "^pkgrel=" "$file"; then
        sed -i "" "s/^pkgrel=.*/pkgrel=\"1\"/" "$file"
        echo "PKGBUILD: Reset pkgrel to 1"
    fi
}

update_yaml_files() {
    local base_path="carbonio-ws-collaboration-openapi/src/main/resources"
    local yaml_files=("$base_path/api.yaml" "$base_path/asyncapi.yaml")

    for yaml_file in "${yaml_files[@]}"; do
        if [ ! -f "$yaml_file" ]; then
            echo "Warning: $yaml_file not found, skipping..."
            continue
        fi

        # Check if version key exists in the file (under info section)
        if ! grep -A 10 "^info:" "$yaml_file" | grep -q "^[[:space:]]*version:"; then
            echo "Warning: 'version:' not found under 'info:' section in $yaml_file, skipping..."
            continue
        fi

        # Get current version (look for version under info section)
        local current_version=
        current_version=$(grep -A 10 "^info:" "$yaml_file" | grep "^[[:space:]]*version:" | head -1 | sed 's/.*version:[[:space:]]*//' | tr -d '"'"'"'')
        echo "$yaml_file: $current_version -> $NEW_VERSION"

        # Update version (handles both quoted and unquoted values, indented under info)
        sed -i "" "/^info:/,/^[^[:space:]]/ s/^[[:space:]]*version:[[:space:]]*.*/  version: $NEW_VERSION/" "$yaml_file"

        # Update x-supported-versions array
        if grep -q "x-supported-versions:" "$yaml_file"; then
            # Extract current array content (handle indentation)
            local current_array
            current_array=$(grep "x-supported-versions:" "$yaml_file" | sed 's/.*x-supported-versions:[[:space:]]*//')

            # Check if new version already exists in array
            if echo "$current_array" | grep -q "\"$NEW_VERSION\""; then
                echo "$yaml_file: Version $NEW_VERSION already exists in x-supported-versions, skipping..."
            else
                # Remove brackets and quotes, split into array
                local versions
                versions=$(echo "$current_array" | tr -d '[]"' | tr ',' '\n' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')

                # Create new array with new version at the beginning
                local new_array="[\"$NEW_VERSION\""
                while IFS= read -r version; do
                    if [ -n "$version" ]; then
                        new_array="$new_array, \"$version\""
                    fi
                done <<< "$versions"
                new_array="$new_array]"

                # Update the file (preserve indentation)
                sed -i "" "s|\(^[[:space:]]*\)x-supported-versions:.*|\1x-supported-versions: $new_array|" "$yaml_file"
                echo "$yaml_file: Added $NEW_VERSION to x-supported-versions"
            fi
        else
            echo "Warning: 'x-supported-versions:' not found in $yaml_file, skipping..."
        fi
    done
}

show_summary() {
    echo ""
    echo "=== Summary ==="

    local pkgbuild_file="package/PKGBUILD"
    if [ -f "$pkgbuild_file" ] && grep -q "^pkgver=" "$pkgbuild_file"; then
        local pkgver
        pkgver=$(grep "^pkgver=" "$pkgbuild_file" | cut -d'=' -f2)
        echo "PKGBUILD pkgver: $pkgver"

        if grep -q "^pkgrel=" "$pkgbuild_file"; then
            local pkgrel=
            pkgrel=$(grep "^pkgrel=" "$pkgbuild_file" | cut -d'=' -f2)
            echo "PKGBUILD pkgrel: $pkgrel"
        fi
    fi

    local base_path="carbonio-ws-collaboration-openapi/src/main/resources"
    local yaml_files=("$base_path/api.yaml" "$base_path/asyncapi.yaml")

    for yaml_file in "${yaml_files[@]}"; do
        if [ -f "$yaml_file" ]; then
            if grep -A 10 "^info:" "$yaml_file" | grep -q "^[[:space:]]*version:"; then
                local version
                version=$(grep -A 10 "^info:" "$yaml_file" | grep "^[[:space:]]*version:" | head -1 | sed 's/.*version:[[:space:]]*//' | tr -d '"'"'"'')
                echo "$yaml_file version: $version"
            fi

            if grep -q "x-supported-versions:" "$yaml_file"; then
                local supported_versions
                supported_versions=$(grep "x-supported-versions:" "$yaml_file" | sed 's/.*x-supported-versions:[[:space:]]*//')
                echo "$yaml_file x-supported-versions: $supported_versions"
            fi
        fi
    done
}

echo "Bumping version to: $NEW_VERSION"
echo ""

update_pkgbuild
update_yaml_files

show_summary
echo ""
echo "Version bump completed successfully!"
