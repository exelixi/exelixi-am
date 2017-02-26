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

#ifdef THROWABLES
#include <setjmp.h>

#define TRY do{ jmp_buf ex_buf__; if( !setjmp(ex_buf__) ){
#define CATCH } else {
#define ETRY } }while(0)
#define THROW longjmp(ex_buf__, 1)

#endif


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
 * Parse an integer from a string.
 * Return 0 (error) or 1 (parsing without errors)
 */
int parse_int(const char *s, int *value) {
	int ret_value = 1;
#ifdef THROWABLES
	TRY{ s = trim(s); *value = strtol(s, NULL, 0); }
	CATCH{ ret_value = 0; } ETRY;
	return ret_value;
#else
    s = trim(s);
    *value = strtol(s, NULL, 0);
    return ret_value;
#endif
}


/**
 * Parse an unsigned integer from a string.
 * Return 0 (error) or 1 (parsing without errors)
 */
int parse_uint(const char *s, unsigned int *value){
	int ret_value = 1;
#ifdef THROWABLES
	TRY{ s = trim(s); *value = strtoul(s, NULL, 0); }
	CATCH{ ret_value = 0; } ETRY;
	return ret_value;
#else
    s = trim(s);
    *value = strtoul(s, NULL, 0);
    return ret_value;
#endif
}

/**
 * Parse a double from a string.
 * Return 0 (error) or 1 (parsing without errors)
 */
int parse_double(const char *s, double *value){
	int ret_value = 1;
	char *ptr;
#ifdef THROWABLES
	TRY{ s = trim(s); *value = strtod(s, &ptr); }
	CATCH{ ret_value = 0; } ETRY;
	return ret_value;
#else
    s = trim(s);
    *value = strtod(s, &ptr);
    return ret_value;
#endif
}



/**
 * Parse a float from a string.
 * Return 0 (error) or 1 (parsing without errors)
 */
int parse_float(const char *s, float *value){
	int ret_value = 1;
	char *ptr;
#ifdef THROWABLES
	TRY{ s = trim(s); *value = strtof(s, &ptr); }
	CATCH{ ret_value = 0; } ETRY;
	return ret_value;
#else
    s = trim(s);
    *value = strtof(s, &ptr);
    return ret_value;
#endif
}


/**
 * Read an integer value from a file
 * Return 0 (error) or 1 (parsing without errors)
 */
int read_int(FILE* fp, int *value) {
	char * line;
	size_t len = 0;
	if (getline(&line, &len, fp) != -1) {
		return parse_int(line, value);
	} else {
		return 0;
	}
}


/**
 * Read an unsigned value from a file
 * Return 0 (error) or 1 (parsing without errors)
 */
int read_uint(FILE* fp, unsigned int *value) {
	char * line;
	size_t len = 0;
	if (getline(&line, &len, fp) != -1) {
		return parse_uint(line, value);
	} else {
		return 0;
	}
}


/**
 * Read a double value from a file
 * Return 0 (error) or 1 (parsing without errors)
 */
int read_double(FILE* fp, double *value){
	char * line;
	size_t len = 0;
	if (getline(&line, &len, fp) != -1) {
		return parse_double(line, value);
	} else {
		return 0;
	}
}


/**
 * Read a float value from a file
 * Return 0 (error) or 1 (parsing without errors)
 */
int read_float(FILE* fp, float *value){
	char * line;
	size_t len = 0;
	if (getline(&line, &len, fp) != -1) {
		return parse_float(line, value);
	} else {
		return 0;
	}
}

/**
 * Write an integer value to the file
 * Each integer is write and a new line created
 */
int write_int(FILE* fp, int value){
    return fprintf(fp, "%d\n", value);
}

/**
 * Write an unsigned integer value to the file
 * Each integer is write and a new line created
 */
int write_uint(FILE* fp, unsigned int value){
    return fprintf(fp, "%u\n", value);
}

/**
 * Write a double value to the file
 * Each integer is write and a new line created
 */
int write_double(FILE* fp, double value){
    return fprintf(fp, "%lf\n", value);
}

/**
 * Write a float value to the file
 * Each integer is write and a new line created
 */
int write_float(FILE* fp, float value){
    return fprintf(fp, "%f\n", value);
}

#endif /* HOST_INCLUDE_UTILS_H_ */
