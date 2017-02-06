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
 * Status can be 0 (error) or 1 (parsing without errors)
 */
int parse_int(const char *s, int *status) {
	s = trim(s);
	*status = strlen(s) ? 1 : 0;
	return strtol(s, NULL, 0);
}

#endif /* HOST_INCLUDE_UTILS_H_ */
