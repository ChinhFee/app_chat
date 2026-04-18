/**
 * @file       <file_name>.c
 * @copyright  Copyright (C) 2019 ITRVN. All rights reserved.
 * @license    This project is released under the Fiot License.
 * @version    major.minor.patch
 * @date       yyyy-mm-dd
 * @author     <first_name_1> <last_name_1>
 * @author     <first_name_2> <last_name_2>
 *             
 * @brief      <A brief description of the content of the file>
 *             
 * @note          
 * @example    example_file_1.c
 *             Example_1 description
 * @example    example_file_2.c
 *             Example_2 description
 */

/* Includes ----------------------------------------------------------- */
#include "code_template.h"

/* Private defines ---------------------------------------------------- */
#define PRIVATE_DEFINE_1  (0) /*!< Description of PRIVATE_DEFINE_1 */
#define PRIVATE_DEFINE_2  (0) /*!< Description of PRIVATE_DEFINE_2 */

/* Private enumerate/structure ---------------------------------------- */
/**
 * @brief <enum descriptiton>
 */
typedef enum 
{
  PRIVATE_ENUM_1, /**< Description of PRIVATE_ENUM_1 */
  PRIVATE_ENUM_2, /**< Description of PRIVATE_ENUM_2 */
  PRIVATE_ENUM_3  /**< Description of PRIVATE_ENUM_3 */
}
private_enum_type_t;

/**
 * @brief <structure descriptiton>
 */
typedef struct 
{
  uint32_t member_1, /**< Description of member_1 */
  uint32_t member_2, /**< Description of member_2 */
  uint32_t member_3  /**< Description of member_3 */
}
private_struct_type_t;

/* Private macros ----------------------------------------------------- */
/**
 * @brief  <macro description>
 *
 * @param[in]     <param_name>  <param_despcription>
 * @param[out]    <param_name>  <param_despcription>
 * @param[inout]  <param_name>  <param_despcription>
 *
 * @attention  <API attention note>
 *
 * @return  
 *  - 0: Success
 *  - 1: Error
 */
#define PRIVATE_MACRO(a)  do_something_with(a)

/* Public variables --------------------------------------------------- */
int g_var_1;
int g_var_2;

/* Private variables -------------------------------------------------- */
static int var_1; /**< Description of private variable var_1 */
static int var_2; /**< Description of private variable var_2 */

/* Private function prototypes ---------------------------------------- */
/**
 * @brief  <function description>
 *
 * @param[in]     <param_name>  <param_despcription>
 * @param[out]    <param_name>  <param_despcription>
 * @param[inout]  <param_name>  <param_despcription>
 *
 * @attention  <API attention note>
 *
 * @return  
 *  - 0: Success
 *  - 1: Error
 */
static void private_function(void);
/* Function definitions ----------------------------------------------- */
void public_function(void)
{
}
/* Private definitions ----------------------------------------------- */
static void private_function(void)
{
}
/* End of file -------------------------------------------------------- */
