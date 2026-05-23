# XMP Taxa Translator

A small utility for translating **Naturalist21 classification labels** inside XMP files into multiple languages.

This tool scans a directory of XMP files and translates taxonomy-related metadata from English into a target language. It supports caching to improve performance on repeated runs.

The project is distributed as a Docker image for easy and consistent execution.

---

## What it does

- Reads `.xmp` files from a directory
- Translates Naturalist21 taxonomy classifications from English. English value is searched in the `lr:hierarchicalSubject` tag.
- Updates XMP metadata with translated values (updates `dc:description` and `lr:hierarchicalSubject`)

##  Usage

Run the container:

```bash
docker run --rm -v /path/to/xmp/files:/data
-v /path/to/xmp/files:/data
ghcr.io/madlemon/xmptaxatranslator:latest
--xmp-dir /data
```


## Program Arguments

### `--xmp-dir` (required)

Path to the directory containing `.xmp` files.

```bash
--xmp-dir /data
```


### `--locale` (optional)

Target language for translation.

Default: `de`

Example:

```bash
--locale fr
```


### `--cache-file` (optional)

Path to a JSON cache file used to store previously translated values.

This improves performance by avoiding repeated translation lookups.

Example:

```bash
--cache-file /data/cache.json
```


## Example with all options

```bash

docker run --rm
-v $(pwd)/xmp:/data
-v $(pwd)/cache.json:/cache.json
ghcr.io/madlemon/xmptaxatranslator:latest
--xmp-dir /data
--locale en
--cache-file /cache.json

```

## External API (iNaturalist)

This tool relies on the iNaturalist API to fetch and translate taxonomy data.

API documentation: https://api.inaturalist.org/v1/

- The API is subject to rate limits
- Large batch operations should use `--cache-file` to reduce requests
- Network connectivity is required during processing

## Requirements

- Docker installed
- Network connectivity


## ⚠️ Notes

- This tool modifies XMP metadata in place
- Always back up files before running batch operations
- Designed for photography workflows and metadata automation

---

## License

This project is licensed under the GLWTS License - see the LICENSE file for details.

