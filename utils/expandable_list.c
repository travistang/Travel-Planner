#include <assert.h>
#include <stdio.h>
#include <stdlib.h>

typedef struct {
	const size_t objectSize;
	void* items;
	size_t count;
} ExpandableList;

ExpandableList* init_list(size_t objectSize)
{
	assert(objectSize > 0);
	ExpandableList* list = malloc(sizeof(ExpandableList));
	// https://stackoverflow.com/questions/9691404/how-to-initialize-const-in-a-struct-in-c-with-malloc
	*(size_t*)&list -> objectSize = objectSize;
	list -> count = 0;
	list -> items = NULL;
	return list;
}

void clean_list(ExpandableList* list)
{
	free(list -> items);
	free(list);
}

size_t is_valid_index(ExpandableList* list, size_t ind)
{
	if(list == NULL) return 0;
	if(ind < 0 || ind >= list -> count) return 0;
	return 1;
}
void* get_item(ExpandableList* list,size_t ind)
{
	assert(is_valid_index(list,ind));
	return &(list -> items[list -> objectSize * ind]);
}

ExpandableList* set_item(ExpandableList* list,size_t pos, void* item)
{
	assert(is_valid_index(list,pos));
	memcpy(get_item(list,pos),item,list -> objectSize);
	return list;
}
ExpandableList* expand(ExpandableList* list, size_t size)
{
	assert(size > 0);
	list -> items = realloc(list -> items,list -> objectSize * (list -> count + size));
	assert(list -> items != NULL); // successfully expanded or not
	list -> count += size;
	return list;
}
ExpandableList* add_item(ExpandableList* list,void* item)
{
	list = expand(list,1);
	set_item(list,list -> count - 1,item);
	return list;
}
ExpandableList* append(ExpandableList* list,ExpandableList* other)
{
	assert(list != NULL
		&& other != NULL
		&& list -> objectSize == other -> objectSize);

	for( size_t i = 0; i < other -> count; ++i)
		add_item(list,get_item(other,i));

	return list;
}

ExpandableList* append_data(ExpandableList* list,void* data, size_t size)
{
	assert(list != NULL);
	size_t count = list -> count,
			objectSize = list -> objectSize;
	list -> count += size / objectSize;
	// expand the new data to a suitable size
	list -> items = realloc(list -> items, count * objectSize +  size);
	assert(list -> items != NULL);
	// append the new data
	memcpy(&(list -> items[count * objectSize]),data,size);
	return list;
}

ExpandableList* clone(ExpandableList* list)
{
		assert(list != NULL);
		ExpandableList* result = init_list(list -> objectSize);
		result -> count = list -> count;
		if (list -> count > 0)
		{
			long totalSize = list -> objectSize * list -> count;
			result -> items = malloc(totalSize);
			memcpy(result -> items, list -> items,totalSize);
		}
		return result;
}

// inclusive in-place slice
ExpandableList* slice_in_place(ExpandableList* list,size_t from, size_t to)
{
	assert(is_valid_index(list,from));
	assert(is_valid_index(list,to));
	assert(from <= to);
	long newSize = (to + 1 - from) * list -> objectSize; // inclusive
	void* newItems = malloc(newSize);
	memcpy(newItems,get_item(list,from),newSize);

	free(list -> items);
	list -> count = to + 1 - from;
	list -> items = newItems;
	return list;
}

ExpandableList* slice(ExpandableList* list,size_t from,size_t to)
{
	ExpandableList* newList = clone(list);
	return slice_in_place(newList,from,to);
}

ExpandableList* delete_item(ExpandableList* list,size_t pos)
{
	assert(is_valid_index(list,pos));
	if (list -> count == 1)
	{
		// will get an empty list...
		free(list -> items);
		list -> count = 0;
		list -> items = NULL;
		return list;
	}
	size_t itemSize = list -> objectSize;
	void* newItems = malloc(itemSize * (list -> count - 1));
	for(size_t i = 0,j = 0; i < list -> count; ++i,++j) // i is tracking the index of old items; j is for the new items.
	{
		if(i == pos)
		{
			// do no copy the items;
			--j; // compensate the increment of the loop
			continue;
		}
		memcpy(
			newItems + j * itemSize,
			list -> items + i * itemSize,
			itemSize);
	}
	free(list -> items);
	list -> items = newItems; // replace the old items with those new ones.
	list -> count --; // decrement the counter to make it consistent
	return list;
}

void for_each(ExpandableList* list, void(*func)(void* const))
{
	for(size_t i = 0; i < list -> count; i++)
	{
		void* const ptr = get_item(list,i);
		func(ptr);
	}
}
ExpandableList* map(ExpandableList* list, void*(*func)(void* const))
{
	ExpandableList* result = init_list(list -> objectSize);
	for(size_t i = 0; i < list -> count; i++)
	{
		add_item(result,func((void* const)get_item(list,i)));
	}
	return result;
}

ExpandableList* reduce(
	ExpandableList* list,
	size_t(*func)(const ExpandableList* const,void* const),
	size_t reduceSize)
{
	ExpandableList* immediate = init_list(reduceSize);
	for(size_t i = 0; i < list -> count; i++)
	{
		add_item(
			immediate,
			func(immediate,
				get_item(list,i)
			)
		);
	}
	return immediate;
}
/*
	Aux. func
*/
void display_char(void* const item)
{
	printf("%c",*(char*)item);
}
void display_int(void* const item)
{
	printf("%d",*(int*)item);
}

void show_char(ExpandableList* list)
{
	for_each(list,display_char);
	printf("\n");
}

void show_int(ExpandableList* list)
{
	for_each(list,display_int);
	printf("\n");
}

void* square(void* const num)
{
	*(int*)num*=*(int*)num; //uh...
	return (void*) num;
}

#define DEBUG 0
#if DEBUG
int main()
{
	ExpandableList* list = init_list(sizeof(int));

	for(int i = 0; i < 5; i++)
	{
		add_item(list,&i);
	}

	show_int(list);
	int a[] = {1,2,3};
	append_data(list,(void*)a,3 * sizeof(int));
	show_int(list);
	clean_list(list);
	return 0;
}
#endif
