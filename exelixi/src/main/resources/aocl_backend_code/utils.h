/*
 * utils.h
 *
 *	A collection of generic utility functions
 *
 *  Created on: Jan 30, 2017
 *      Author: Simone Casale-Brunet
 */

#ifndef HOST_INCLUDE_UTILS_H_
#define HOST_INCLUDE_UTILS_H_

#include <string.h>
#include "string"

/**
 * Trim a string
 */
char * trim(const char * s) {
	int l = strlen(s);

	while (isspace(s[l - 1]))
		--l;
	while (*s && isspace(*s))
		++s, --l;

	return strndup(s, l);
}

/**
 * Parse an int from a string.
 * Return 0 (error) or 1 (parsing without errors)
 */
int parse_int(const char *s, int *value) {
	s = trim(s);
	*value = strtol(s, NULL, 0);
	return strlen(s) ? 1 : 0;
}

/**
 * Read an integer value from a file
 * Return 0 (error) or 1 (parsing without errors)
 */
int read_int_value(FILE* fp, int *value) {
	char * line;
	size_t len = 0;
	if (getline(&line, &len, fp) != -1) {
		return parse_int(line, value);
	} else {
		return 0;
	}
}

/**
 * Write an integer value to the file
 * Each integer is write and a new line created
 */
int write_int_value(FILE* fp, int value){
    return fprintf(fp, "%d\n", value);
}

#endif /* HOST_INCLUDE_UTILS_H_ */
